/**
 * Copyright (c) 2017-present, Team Bucket Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.github.luks91.teambucket

import com.github.luks91.teambucket.model.*
import com.github.luks91.teambucket.main.reviewers.ReviewersPresenter
import com.github.luks91.teambucket.connection.BitbucketApi
import com.github.luks91.teambucket.connection.ConnectionProvider
import com.github.luks91.teambucket.persistence.RepositoriesStorage
import com.github.luks91.teambucket.persistence.TeamMembersStorage
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Timed
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TeamMembersProvider @Inject constructor(val connectionProvider: ConnectionProvider, val membersStorage: TeamMembersStorage,
                                              val repositoriesStorage: RepositoriesStorage) {

    companion object {
        const val PAGES_PER_REPOSITORY = 3L
        const val MINIMUM_USER_OCCURRENCES = 2
        const val PAGE_LIMIT = 25
        const val MEMBERSHIP_TIMEOUT_HOURS = 20L
        const val MEMBERSHIP_PRS_COUNT = 25
    }

    private val teamMembers = calculateTeamMembership().subscribeOn(Schedulers.io())
            .doOnNext{ membersStorage.persistTeamMembers(it) }.replay(1).refCount()

    fun teamMembers(refreshTicks: Observable<Any>): Observable<Map<User, Density>> {
        return refreshTicks.switchMap { _ ->
            membersStorage.teamMembers()
                    .first(Timed(emptyMap(), 0, TimeUnit.MILLISECONDS)).toObservable()
                    .switchMap { members -> if (members.haveExpired()) teamMembers else Observable.just(members) }
        }.map { timedMembers -> timedMembers.value() }
    }

    private fun Timed<Map<User, Density>>.haveExpired(): Boolean {
        return (System.currentTimeMillis() - time()) > TimeUnit.HOURS.toMillis(MEMBERSHIP_TIMEOUT_HOURS) || value().isEmpty()
    }

    private fun calculateTeamMembership(): Observable<Timed<Map<User, Density>>> {
        return Observable.combineLatest(
                connectionProvider.connections(),
                repositoriesStorage.selectedRepositories(),
                BiFunction<BitbucketConnection, List<Repository>, Observable<Timed<Map<User, Density>>>> {
                    (userName, _, api, token), repositories ->
                    return@BiFunction  Observable.fromIterable(repositories)
                            .flatMap { (slug, _, project) ->
                                BitbucketApi.queryPaged { start ->
                                    api.getPullRequests(token, project.key, slug, start,
                                            status = STATUS_ALL, limit = PAGE_LIMIT)
                                }
                                        .subscribeOn(Schedulers.io())
                                        .take(PAGES_PER_REPOSITORY)
                                        .onErrorResumeNext(connectionProvider
                                                .handleNetworkError(ReviewersPresenter::class.java.simpleName))
                            }.reduce { t1, t2 -> t1 + t2 }.toObservable()
                            .compose(intoTeamMembershipOf(userName))
                            .switchIfEmpty(Observable.just(mapOf()))
                            .timestamp()
                }
        ).switchMap { stream -> stream }
    }

    private fun intoTeamMembershipOf(userName: String): ObservableTransformer<List<PullRequest>, Map<User, Density>> {
        return ObservableTransformer { upstream ->
            upstream.map { pullRequests ->
                val densitiesMap = mutableMapOf<User, MutableDensity>()
                var processedUserReviews = 0
                val pullRequestsIterator = pullRequests.sortedByDescending { it.createdDate }.iterator()
                while (pullRequestsIterator.hasNext() && processedUserReviews < MEMBERSHIP_PRS_COUNT) {
                    val pullRequest = pullRequestsIterator.next()
                    val author = pullRequest.author.user
                    val reviewers = pullRequest.reviewers.map { it.user }
                    //if current user is the author, add all the other reviewers
                    if (author.name.equals(userName, ignoreCase = true)) {
                        reviewers.forEach { densitiesMap.getOrPut(it, { MutableDensity() }).inbound++ }
                        processedUserReviews++
                    } else { //if current user is a reviewer, add all the other reviewers and the author
                        var foundUser = false
                        var index = 0
                        var user: User
                        val reviewersIterator = reviewers.iterator()
                        while (!foundUser && reviewersIterator.hasNext()) {
                            user = reviewersIterator.next()
                            if (user.name.equals(userName, ignoreCase = true)) {
                                foundUser = true
                                processedUserReviews++
                                if (index > 0) {
                                    reviewers.subList(0, index)
                                            .forEach { densitiesMap.getOrPut(it, { MutableDensity() }).inbound++ }
                                }
                                if (index < reviewers.size - 1) {
                                    reviewers.subList(index + 1, reviewers.size)
                                            .forEach { densitiesMap.getOrPut(it, { MutableDensity() }).inbound++ }
                                }
                                densitiesMap.getOrPut(author, { MutableDensity() }).outbound++
                            }
                            index++
                        }
                    }
                }

                return@map densitiesMap
                        .filterValues { it.inbound + it.outbound > MINIMUM_USER_OCCURRENCES }
                        .mapValues { Density(it.value.inbound, it.value.outbound) }
            }
        }
    }

    private class MutableDensity(var inbound: Int = 0, var outbound: Int = 0)
}

fun Map<User, Density>.getLeadUser(pullRequests: List<PullRequest>): User? {
    var leadParam = 0.0
    var leadUser: User? = null
    for ((user, density) in this) {
        val currentUserParam = 1.0 * density.inbound / Math.max(1, density.outbound)
        if (currentUserParam > leadParam) {
            leadParam = currentUserParam
            leadUser = user
        }
    }

    return if (leadParam > 0.75 * Math.min(TeamMembersProvider.MEMBERSHIP_PRS_COUNT, pullRequests.size)) leadUser else null
}