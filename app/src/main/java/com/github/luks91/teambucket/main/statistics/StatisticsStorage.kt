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

import com.github.luks91.teambucket.model.EMPTY_STRING
import com.github.luks91.teambucket.model.PullRequest
import com.github.luks91.teambucket.model.PullRequestActivity
import com.github.luks91.teambucket.persistence.*
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.apache.commons.lang3.StringUtils
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
}

//this class needed to have copied code from RealmPullRequest, due to https://github.com/realm/realm-java/issues/761
@RealmClass
internal open class RealmDetailedPullRequest(open var id: Long = 0,
                                            open var title: String = EMPTY_STRING,
                                            open var createdDate: Long = 0L,
                                            open var updatedDate: Long = 0L,
                                            open var author: RealmPullRequestMember = RealmPullRequestMember(),
                                            open var reviewers: RealmList<RealmPullRequestMember> = RealmList(),
                                            open var state: String = EMPTY_STRING,
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

