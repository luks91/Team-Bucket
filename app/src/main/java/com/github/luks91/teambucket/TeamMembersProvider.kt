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
import com.github.luks91.teambucket.persistence.PersistenceProvider
import com.github.luks91.teambucket.main.reviewers.ReviewersPresenter
import com.github.luks91.teambucket.connection.BitbucketApi
import com.github.luks91.teambucket.connection.ConnectionProvider
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Timed
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TeamMembersProvider @Inject constructor(val connectionProvider: ConnectionProvider,
                                              val persistenceProvider: PersistenceProvider,
                                              val eventsBus: ReactiveBus) {

    private companion object Util {
        const val PAGES_PER_REPOSITORY = 1L
        const val MINIMUM_USER_OCCURRENCES = 2
        const val PAGE_LIMIT = 20
        const val MEMBERSHIP_TIMEOUT_HOURS = 20L
    }

    fun teamMembers(refreshTicks: Observable<Any>): Observable<List<User>> {
        val remoteMembership = calculateTeamMembership().publish()
        return refreshTicks.switchMap { _ ->
            persistenceProvider.teamMembers()
                    .switchMap { members -> if (members.haveExpired()) remoteMembership.refCount() else Observable.just(members) }
        }.map { timedMembers -> timedMembers.value() }

                //use mergeWith to bind to subscribe/unsubscribe lifecycle of the upstream observable
                .mergeWith(persistenceProvider.teamMembersPersisting(remoteMembership).concatMap { Observable.empty<List<User>>() })
    }

    private fun Timed<List<User>>.haveExpired(): Boolean {
        return (System.currentTimeMillis() - time()) > TimeUnit.HOURS.toMillis(MEMBERSHIP_TIMEOUT_HOURS) || value().isEmpty()
    }

    private fun calculateTeamMembership(): Observable<Timed<List<User>>> {
        return Observable.combineLatest(
                connectionProvider.obtainConnection(),
                persistenceProvider.selectedRepositories(),
                BiFunction<BitbucketConnection, List<Repository>, Observable<Timed<List<User>>>> {
                    (userName, _, api, token), repositories ->
                    return@BiFunction  Observable.fromIterable(repositories)
                            .flatMap { (slug, _, project) ->
                                BitbucketApi.queryPaged { start ->
                                    api.getPullRequests(token, project.key, slug, start,
                                            status = STATUS_ALL, limit = PAGE_LIMIT)
                                }
                                        .subscribeOn(Schedulers.io())
                                        .take(PAGES_PER_REPOSITORY)
                                        .onErrorResumeNext(BitbucketApi.handleNetworkError(eventsBus,
                                                ReviewersPresenter::class.java.simpleName))
                            }.reduce { t1, t2 -> t1 + t2 }.toObservable()
                            .compose(intoTeamMembershipOf(userName))
                            .switchIfEmpty(Observable.just(listOf()))
                            .timestamp()
                }
        ).switchMap { stream -> stream }
    }

    private fun intoTeamMembershipOf(userName: String): ObservableTransformer<List<PullRequest>, List<User>> {
        return ObservableTransformer { upstream ->
            upstream.map { pullRequests ->
                val team = mutableListOf<User>()
                for (pullRequest in pullRequests) {
                    val author = pullRequest.author.user
                    val reviewers = pullRequest.reviewers.map { it.user }
                    //if current user is the author, add all the other reviewers
                    if (author.name.equals(userName, ignoreCase = true)) {
                        team += reviewers
                    } else { //if current user is a reviewer, add all the other reviewers and the author
                        var foundUser = false
                        var index = 0
                        var user: User
                        val reviewersIterator = reviewers.iterator()
                        while (!foundUser && reviewersIterator.hasNext()) {
                            user = reviewersIterator.next()
                            if (user.name.equals(userName, ignoreCase = true)) {
                                foundUser = true
                                if (index > 0) {
                                    team += reviewers.subList(0, index)
                                }
                                if (index < reviewers.size - 1) {
                                    team += reviewers.subList(index + 1, reviewers.size)
                                }
                                team += author
                            }
                            index++
                        }
                    }
                }

                return@map team.groupBy { key -> key }.filter { it.value.size >= MINIMUM_USER_OCCURRENCES }
                        .values.map { it -> it[0] }
            }
        }
    }
}