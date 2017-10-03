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

package com.github.luks91.teambucket.main.statistics

import android.util.Log
import com.github.luks91.teambucket.model.*
import com.github.luks91.teambucket.persistence.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.Sort
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StatisticsStorage @Inject constructor(){
    private val scheduler by RealmSchedulerHolder
    companion object {
        const val STATISTICS_REALM = "statistics.realm"
    }

    fun subscribePersistingStatistics(detailedPullRequests: Observable<Observable<DetailedPullRequest>>): Disposable {
        return detailedPullRequests
                .concatMap { window ->
                    usingRealm(STATISTICS_REALM, scheduler) { realm ->
                        window.observeOn(scheduler)
                                .map { RealmDetailedPullRequest.from(it) }
                                .toList()
                                .map { realmList -> realm.executeTransaction { realm.copyToRealmOrUpdate(realmList) } }
                                .toObservable()
                    }
                }.subscribe()
    }

    fun timeToRefreshFor(projectKey: String, repoSlug: String): Pair<Long, TimeUnit> {
        return usingRealm(STATISTICS_REALM, scheduler) { realm ->
                Log.i("TMPTAG", "Data: " + projectKey + " " + repoSlug)
                val result = realm.where(RealmDetailedPullRequest::class.java)
                        //.equalTo("targetBranch.repository.project.key", "projectKey")
                        .equalTo("targetBranch.repository.slug", repoSlug)
                        .equalTo("state", "OPEN")
                        .findAllSorted("createdDate", Sort.ASCENDING)
                        .map { (System.currentTimeMillis() - it.createdDate) to TimeUnit.MILLISECONDS }
                        .firstOrNull()

                return@usingRealm if (result != null) Observable.just(result) else Observable.empty()
        }.first(90L to TimeUnit.DAYS)
                .doOnSuccess { Log.i("TMPTAG", "Starting sync from: " + it) }
                .blockingGet()
    }

    fun statisticsFrom(value: Long, unit: TimeUnit): Observable<List<DetailedPullRequest>> {
        val takeFrom = System.currentTimeMillis() - unit.toMillis(value)
        return usingRealm(STATISTICS_REALM, scheduler) { realm ->
            realm.where(RealmDetailedPullRequest::class.java)
                    .greaterThan("createdDate", takeFrom)
                    .or()
                    .greaterThan("updatedDate", takeFrom)
                    .findAll().asFlowable()
                    .concatMap { detailedPullRequests -> Flowable.fromIterable(detailedPullRequests)
                            .map { detailedPullRequest -> detailedPullRequest.toDetailedPullRequest() }
                            .toList().toFlowable()
                    }.toObservable()
        }
    }
}

//this class needed to have copied code from RealmPullRequest, due to https://github.com/realm/realm-java/issues/761
@RealmClass
internal open class RealmDetailedPullRequest(open var id: Long = 0,
                                             open var title: String = EMPTY_STRING,
                                             open var createdDate: Long = 0L,
                                             open var updatedDate: Long = 0L,
                                             open var author: RealmPullRequestMember = RealmPullRequestMember(),
                                             open var reviewers: RealmList<RealmPullRequestMember> = RealmList(),
                                             @PullRequestStatus open var state: String = EMPTY_STRING,
                                             open var sourceBranch: RealmGitReference = RealmGitReference(),
                                             open var targetBranch: RealmGitReference = RealmGitReference(),
                                             open var activities: RealmList<RealmPullRequestActivity> = RealmList()): RealmModel {

    @PrimaryKey open var realmId: String = "${id}_${sourceBranch?.repository?.slug}_${sourceBranch?.repository?.project?.key}"

    companion object {
        fun from(detailedPr: DetailedPullRequest) = RealmDetailedPullRequest(
                detailedPr.pullRequest.id, detailedPr.pullRequest.title, detailedPr.pullRequest.createdDate,
                detailedPr.pullRequest.updatedDate, RealmPullRequestMember.from(detailedPr.pullRequest.author),
                detailedPr.pullRequest.reviewers.map { RealmPullRequestMember.from(it) }.toRealmList(),
                detailedPr.pullRequest.state, RealmGitReference.from(detailedPr.pullRequest.sourceBranch),
                RealmGitReference.from(detailedPr.pullRequest.targetBranch),
                detailedPr.activities.map { RealmPullRequestActivity.from(it) }.toRealmList())
    }

    fun toDetailedPullRequest() = DetailedPullRequest(
            PullRequest(id, title, createdDate, updatedDate, author.toPullRequestMember(),
                    reviewers.map(RealmPullRequestMember::toPullRequestMember).toList(),
                    state, sourceBranch.toGitReference(), targetBranch.toGitReference()),
            activities.map { it.toPullRequestActivity() })
}

@RealmClass
internal open class RealmPullRequestActivity(@PrimaryKey open var id: Long = 0,
                                       open var createdDate: Long = 0,
                                       open var user: RealmUser = RealmUser(),
                                       open var action: String = StringUtils.EMPTY): RealmModel {

    companion object {
        fun from(activity: PullRequestActivity) = RealmPullRequestActivity(activity.id,
                activity.createdDate, RealmUser.from(activity.user), activity.action)
    }

    fun toPullRequestActivity() = PullRequestActivity(id, createdDate, user.toUser(), action)
}

