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
import android.net.Uri
import android.webkit.URLUtil
import com.github.luks91.prparadise.R
import com.github.luks91.prparadise.ReviewersView
import com.github.luks91.prparadise.model.*
import com.github.luks91.prparadise.persistence.PersistenceProvider
import com.github.luks91.prparadise.rest.BitbucketApi
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.BiFunction
import io.reactivex.observables.ConnectableObservable
import io.reactivex.schedulers.Schedulers

class ReviewersPresenter(private val context: Context) : MvpPresenter<ReviewersView> {

    private val connectionProvider: ConnectionProvider =
            ConnectionProvider(context, context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE))
    private val persistenceProvider: PersistenceProvider = PersistenceProvider(context)
    private var disposable = Disposables.empty()

    override fun attachView(view: ReviewersView) {
        val pullRequests = pullRequests(view)
        disposable = CompositeDisposable(
                subscribeProvidingReviewers(pullRequests, view),
                persistenceProvider.pullRequestsPersisting(pullRequests.map { (pullRequests) -> pullRequests }),
                pullRequests.connect(),
                subscribeProvidingPullRequests(view),
                subscribeImageLoading(view)
        )
    }

    private fun pullRequests(view: ReviewersView): ConnectableObservable<Pair<List<PullRequest>, String>> {
        return Observable.combineLatest(
                view.intentPullToRefresh().startWith { Object() },
                connectionProvider.obtainConnection(),
                BiFunction<Any, BitbucketConnection, BitbucketConnection> { _, conn -> conn }
        ).switchMap { (serverUrl, api, token) ->
            persistenceProvider.selectedRepositories()
                    .switchMap { list ->
                        Observable.fromIterable(list)
                                .flatMap { (slug, _, project) ->
                                    BitbucketApi.queryPaged { start -> api.getPullRequests(token, project.key, slug, start) }
                                    .subscribeOn(Schedulers.io())
                                    .onErrorResumeNext(BitbucketApi.handleNetworkError(ReviewersPresenter::class.java.simpleName))
                                }.reduce { t1, t2 -> t1 + t2 }.map { list -> Pair(list, serverUrl) }.toObservable()
                                .switchIfEmpty(Observable.just(Pair(listOf(), serverUrl)))
                    }
        }.publish()
    }

    private fun subscribeProvidingReviewers(pullRequests: ConnectableObservable<Pair<List<PullRequest>, String>>,
                                            view: ReviewersView): Disposable {
        return pullRequests
                .map { (pullRequests, serverUrl) -> reviewersInformationFrom(pullRequests, serverUrl) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { view.onLoadingCompleted() }
                .subscribe { prCount -> view.onReviewersReceived(prCount) }
    }

    private fun reviewersInformationFrom(pullRequests: List<PullRequest>, serverUrl: String): ReviewersInformation {
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

    private fun subscribeProvidingPullRequests(view: ReviewersView): Disposable {
        return view.intentRetrieveReviews()
                .switchMap { (userIndex, user) -> persistenceProvider.pullRequestsUnderReviewBy(user)
                        .map { pullRequests -> pullRequests.mapIndexed { i, pr -> IndexedValue(userIndex + 1 + i, pr) } }
                        .first(listOf()).toObservable() //FIXME: Adapter cannot handle live updates ATM.. we need to change that
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list -> view.onPullRequestsProvided(list) }
    }

    private fun subscribeImageLoading(view: ReviewersView): Disposable {
        return view.intentLoadAvatarImage()
                .subscribe { (serverUrl, urlPath, target) ->
                    val requestBuilder = Picasso.with(this@ReviewersPresenter.context)
                    val requestCreator = if (URLUtil.isValidUrl(urlPath)) requestBuilder.load(urlPath)
                            else requestBuilder.load(Uri.parse(serverUrl).buildUpon().appendEncodedPath(urlPath).build())

                    requestCreator.placeholder(R.drawable.ic_sentiment_satisfied_black_24dp)
                            .error(R.drawable.ic_sentiment_very_satisfied_black_24dp)
                            .into(target)
                }
    }

    override fun detachView(retainInstance: Boolean) {
        disposable.dispose()
    }
}
