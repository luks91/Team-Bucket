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

package com.github.luks91.teambucket.main.reviewers

import android.content.Context
import com.github.luks91.teambucket.connection.ConnectionProvider
import com.github.luks91.teambucket.TeamMembersProvider
import com.github.luks91.teambucket.di.AppContext
import com.github.luks91.teambucket.persistence.PullRequestsStorage
import com.github.luks91.teambucket.main.base.BasePullRequestsPresenter
import com.github.luks91.teambucket.persistence.RepositoriesStorage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class ReviewersPresenter @Inject constructor(@AppContext context: Context, connectionProvider: ConnectionProvider,
                                             repositoriesStorage: RepositoriesStorage,
                                             private val pullRequestsStorage: PullRequestsStorage,
                                             teamMembersProvider: TeamMembersProvider)
    : BasePullRequestsPresenter<ReviewersView>(context, connectionProvider, repositoriesStorage, pullRequestsStorage,
        teamMembersProvider) {

    private var disposable = Disposables.empty()

    override fun attachView(view: ReviewersView) {
        super.attachView(view)
        disposable = subscribeProvidingPullRequests(view)
    }

    override fun detachView(retainInstance: Boolean) {
        super.detachView(retainInstance)
        disposable.dispose()
    }

    private fun subscribeProvidingPullRequests(view: ReviewersView): Disposable =
            view.intentRetrieveReviews()
                .switchMap { (userIndex, user) -> pullRequestsStorage.pullRequestsUnderReviewBy(user.slug)
                        .map { pullRequests -> pullRequests.mapIndexed { i, pr -> IndexedValue(userIndex + 1 + i, pr) } }
                        .first(listOf()).toObservable() //FIXME: Adapter cannot handle live updates ATM.. we need to change that
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list -> view.onPullRequestsProvided(list) }
}