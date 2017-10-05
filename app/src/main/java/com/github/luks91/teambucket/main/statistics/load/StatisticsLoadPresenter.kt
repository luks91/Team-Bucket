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

package com.github.luks91.teambucket.main.statistics.load

import com.github.luks91.teambucket.TeamMembersProvider
import com.github.luks91.teambucket.connection.BitbucketApi
import com.github.luks91.teambucket.connection.ConnectionProvider
import com.github.luks91.teambucket.main.statistics.DetailedPullRequest
import com.github.luks91.teambucket.main.statistics.StatisticsStorage
import com.github.luks91.teambucket.model.*
import com.github.luks91.teambucket.persistence.RepositoriesStorage
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import io.reactivex.Observable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StatisticsLoadPresenter @Inject constructor(private val connectionProvider: ConnectionProvider,
                                                  private val teamMembersProvider: TeamMembersProvider,
                                                  private val repositoriesStorage: RepositoriesStorage,
                                                  private val statisticsStorage: StatisticsStorage): MvpPresenter<StatisticsLoadView> {
    private var disposable = Disposables.empty()

    override fun attachView(view: StatisticsLoadView) {
        val pullRequests = Observable.combineLatest(
                connectionProvider.connections(),
                teamMembersProvider.teamMembers(view.intentRefreshData().startWith(Object())).map { it.keys.map { it.slug }
                        .toSet() },
                repositoriesStorage.selectedRepositories(),
                Function3<BitbucketConnection, Set<String>, List<Repository>, Observable<Observable<DetailedPullRequest>>> {
                    (userSlug, _, api, token), teamMembers, repositories ->
                        Observable.fromIterable(repositories)
                            .flatMap { (slug, _, project) ->
                                BitbucketApi.queryPaged { start ->
                                    api.getPullRequests(token, project.key, slug, start, status = STATUS_ALL) }
                                        .concatMapIterable { it }
                                        .filter { it.isWithinTeam(userSlug, teamMembers) }
                                        .takeWhile { it.isCreatedNoLongerThanAgo(10, TimeUnit.DAYS) }
                                        .doOnNext { view.onPullRequestDetected() }
                                        .subscribeOn(Schedulers.computation())
                                        .onErrorResumeNext(connectionProvider.handleNetworkError(
                                                StatisticsLoadPresenter::class.java.simpleName))
                                        .flatMap { pullRequest ->
                                            BitbucketApi.queryPaged { start ->
                                                api.getPullRequestActivities(token, project.key, slug, pullRequest.id, start) }
                                                    .subscribeOn(Schedulers.computation())
                                                    .reduce { t1, t2 -> t1 + t2 }
                                                    .map { DetailedPullRequest(pullRequest, it) }
                                                    .toObservable()
                                                    .doOnComplete { view.onPullRequestProcessed() }
                                        }
                            }.window(50)
                            .doOnSubscribe { view.onLoadingStarted() }
                            .doOnComplete { view.onLoadingCompleted() }
                }
        ).switchMap { obs -> obs }

        disposable = statisticsStorage.subscribePersistingStatistics(pullRequests)
    }

    private fun PullRequest.isCreatedNoLongerThanAgo(value: Long, unit: TimeUnit) =
            this.createdDate >= System.currentTimeMillis() - unit.toMillis(value)

    private fun PullRequest.isWithinTeam(userSlug: String, teamMembersIds: Set<String>) =
            (userSlug == author.user.slug || teamMembersIds.contains(author.user.slug))
                    && reviewers.any { teamMembersIds.contains(it.user.slug) || userSlug == it.user.slug }

    override fun detachView(retainInstance: Boolean) {
        disposable.dispose()
    }
}