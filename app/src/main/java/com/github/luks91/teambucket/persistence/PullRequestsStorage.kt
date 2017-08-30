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

package com.github.luks91.teambucket.persistence

import android.content.Context
import com.github.luks91.teambucket.di.AppContext
import com.github.luks91.teambucket.model.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.realm.Realm
import javax.inject.Inject

class PullRequestsStorage @Inject constructor(@AppContext context: Context) {
    private val scheduler by RealmSchedulerHolder
    companion object Holder {
        const val PULL_REQUESTS_REALM = "pull_requests_realm"
    }

    init {
        Realm.init(context)
    }

    fun pullRequestsPersisting(pullRequests: Observable<List<PullRequest>>): Disposable {
        return usingRealm(PULL_REQUESTS_REALM, scheduler) { realm ->
            pullRequests
                    .observeOn(scheduler)
                    .map { pullRequest -> pullRequest.map(RealmPullRequest.Factory::from) }
                    .map { pullRequests ->
                        realm.executeTransaction {
                            //TODO: removing all the rows and writing them again is not very efficient (though we're not dealing
                            //TODO: with much data as of right now). Consider improving performance in this area.
                            realm.delete(RealmPullRequest::class.java)
                            realm.delete(RealmPullRequestMember::class.java)
                            realm.delete(RealmUser::class.java)
                            realm.delete(RealmGitReference::class.java)
                            realm.copyToRealmOrUpdate(pullRequests)
                        }
                    }
        }.subscribe()
    }

    fun pullRequestsUnderReviewBy(userSlug: String): Observable<List<PullRequest>> {
        return usingRealm(PULL_REQUESTS_REALM, scheduler) { realm ->
            realm.where(RealmPullRequestMember::class.java)
                    .equalTo("user.slug", userSlug)
                    .equalTo("approved", false)
                    .equalTo("role", "REVIEWER")
                    .findAll().asFlowable()
                    .concatMap { reviewers -> Flowable.fromIterable(reviewers)
                            .concatMapIterable{ member -> member.reviewingPullRequests }
                            .map { realmPullRequest -> realmPullRequest.toPullRequest() }
                            .toList().toFlowable()
                    }.toObservable()
        }
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
    }
}