/**
 * Copyright (c) 2017-present, PR Paradise Contributors.
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

package com.github.luks91.prparadise.presenter

import android.content.Context
import com.github.luks91.prparadise.ReviewersView
import com.github.luks91.prparadise.model.*
import com.github.luks91.prparadise.rest.BitbucketApi
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils

class ReviewersPresenter(context: Context) : MvpPresenter<ReviewersView> {

    private val connectionProvider: ConnectionProvider =
            ConnectionProvider(context, context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE))
    private val repositoriesProvider: RepositoriesProvider = RepositoriesProvider()
    private var subscription = Disposables.empty()

    override fun attachView(view: ReviewersView) {
        subscription = Observable.combineLatest(view.intentPullToRefresh().startWith { Object() },
                connectionProvider.obtainConnection(),
                BiFunction<Any, BitbucketConnection, BitbucketConnection> { _, conn -> conn })
                .observeOn(Schedulers.io())
                .switchMap { (serverUrl, api, token) ->
                    repositoriesProvider.obtainSelectedRepositories()
                            .flatMap { repositories ->
                                Observable.fromIterable(repositories).flatMap { (slug, _, project) ->
                                    BitbucketApi.queryPaged { start -> api.getPullRequests(token, project.key, slug, start)
                                                .subscribeOn(Schedulers.io())
                                    }
                                    .subscribeOn(Schedulers.io())
                                    .onErrorResumeNext(BitbucketApi.handleNetworkError(ReviewersPresenter::class.java.simpleName))
                                }
                                .reduce { t1, t2 -> t1 + t2 }
                                .map { values -> reviewersInformationFrom(values, serverUrl) }.toObservable()
                                .first(ReviewersInformation(listOf<Reviewer>(), StringUtils.EMPTY)).toObservable()
                            }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { view.onLoadingCompleted() }
                .subscribe { prCount -> view.onReviewersReceived(prCount) }
    }

    fun reviewersInformationFrom(pullRequests: List<PullRequest>, serverUrl: String): ReviewersInformation {
        val usersToPullRequests = mutableMapOf<User, Int>()
        pullRequests.forEach {
            it.reviewers
                    .filterNot { it.approved }
                    .forEach {
                        if (!usersToPullRequests.contains(it.user)) {
                            usersToPullRequests[it.user] = 1
                        } else {
                            usersToPullRequests[it.user] = usersToPullRequests[it.user]!! + 1
                        }
                    }
        }

        val returnList = mutableListOf<Reviewer>()
        for ((key, value) in usersToPullRequests) {
            returnList.add(Reviewer(user = key, reviewsCount = value))
        }

        return ReviewersInformation(returnList.sortedWith(compareBy({ it.reviewsCount }, { it.user.displayName })), serverUrl)
    }

    override fun detachView(retainInstance: Boolean) {
        subscription.dispose()
    }
}
