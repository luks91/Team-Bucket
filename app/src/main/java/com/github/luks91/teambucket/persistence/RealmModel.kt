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

import com.github.luks91.teambucket.model.*
import io.reactivex.schedulers.Timed
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.TimeUnit

//We don't want to expose mutable model outside of this usage. All the modules and streams have to work with mutable
//model classes specified in .model package. As of Q2 2017, realm does not work with Kotlin data classes.
@RealmClass
internal open class RealmProject(@PrimaryKey open var key: String = EMPTY_STRING,
                                 open var name: String = EMPTY_STRING,
                                 open var description: String? = EMPTY_STRING) : RealmModel {
    fun toProject(): Project {
        return Project(key, name, description)
    }

    companion object Factory {
        fun from(project: Project): RealmProject {
            return RealmProject(project.key, project.name, project.description)
        }
    }
}

@RealmClass
internal open class RealmRepository(
        open var slug: String = EMPTY_STRING,
        open var name: String = EMPTY_STRING,
        open var project: RealmProject = RealmProject()) : RealmModel {

    @PrimaryKey open var realmId: String = "${project?.key}_$slug"

    fun toRepository(): Repository {
        return Repository(slug, name, project.toProject())
    }

    companion object Factory {
        fun from(repository: Repository): RealmRepository {
            return RealmRepository(repository.slug, repository.name, RealmProject.from(repository.project))
        }
    }
}

@RealmClass
internal open class RealmTeam(@PrimaryKey open var timestampMillis: Long = 0,
                              open var membersList: RealmList<RealmDensifiedUser> = RealmList<RealmDensifiedUser>()) :
        RealmModel {

    fun toTimedMap(): Timed<Map<User, Density>> {
        val membersMap = membersList.associateBy ({ it.user.toUser() }, { Density(it.inbound, it.outbound) })
        return Timed(membersMap, timestampMillis, TimeUnit.MILLISECONDS)
    }

    companion object {
        fun from(map: Timed<Map<User, Density>>) = RealmTeam(map.time(),
                map.value().map { RealmDensifiedUser.from(it.key, it.value) }.toRealmList())
    }

}

internal fun <T: RealmModel> Iterable<T>.toRealmList(): RealmList<T> = RealmList<T>().apply { addAll(this@toRealmList) }

@RealmClass
internal open class RealmDensifiedUser(open var user: RealmUser = RealmUser(),
                                       open var inbound: Int = 0,
                                       open var outbound: Int = 0) : RealmModel {

    companion object {
        fun from(user: User, density: Density) = RealmDensifiedUser(RealmUser.from(user), density.inbound, density.outbound)
    }
}

@RealmClass
internal open class RealmUser(@PrimaryKey open var id: Long = 0,
                              open var name: String = EMPTY_STRING,
                              open var displayName: String = EMPTY_STRING,
                              open var slug: String = EMPTY_STRING,
                              open var avatarUrlSuffix: String? = EMPTY_STRING) : RealmModel {

    fun toUser(): User {
        return User(id, name, displayName, slug, avatarUrlSuffix.orEmpty())
    }

    companion object Factory {
        fun from(user: User): RealmUser {
            return RealmUser(user.id, user.name, user.displayName, user.slug, user.avatarUrlSuffix.orEmpty())
        }
    }
}

@RealmClass
internal open class RealmPullRequestMember(open var user: RealmUser = RealmUser(),
               open var role: String = EMPTY_STRING,
               open var approved: Boolean = false,
               open var status: String = EMPTY_STRING,
               @LinkingObjects("reviewers") open val reviewingPullRequests: RealmResults<RealmPullRequest>? = null) : RealmModel {

    fun toPullRequestMember(): PullRequestMember {
        return PullRequestMember(user.toUser(), role, approved, status)
    }

    companion object Factory {
        fun from(member: PullRequestMember): RealmPullRequestMember {
            return RealmPullRequestMember(RealmUser.from(member.user), member.role, member.approved, member.status)
        }
    }
}

@RealmClass
internal open class RealmGitReference(@PrimaryKey open var displayId: String = StringUtils.EMPTY,
                                      open var latestCommit: String = StringUtils.EMPTY,
                                      open var repository: RealmRepository = RealmRepository()): RealmModel {

    fun toGitReference(): GitReference {
        return GitReference(displayId, latestCommit, repository.toRepository())
    }

    companion object Factory {
        fun from(gitReference: GitReference): RealmGitReference {
            return RealmGitReference(gitReference.displayId, gitReference.latestCommit,
                    RealmRepository.from(gitReference.repository))
        }
    }
}

@RealmClass
internal open class RealmPullRequest(open var id: Long = 0,
                                     open var title: String = EMPTY_STRING,
                                     open var createdDate: Long = 0L,
                                     open var updatedDate: Long = 0L,
                                     open var author: RealmPullRequestMember = RealmPullRequestMember(),
                                     open var reviewers: RealmList<RealmPullRequestMember> = RealmList(),
                                     open var state: String = EMPTY_STRING,
                                     open var sourceBranch: RealmGitReference = RealmGitReference(),
                                     open var targetBranch: RealmGitReference = RealmGitReference()) : RealmModel {

    @PrimaryKey open var realmId: String = "${id}_${sourceBranch?.repository?.slug}_${sourceBranch?.repository?.project?.key}"

    fun toPullRequest(): PullRequest {
        return PullRequest(id, title, createdDate, updatedDate, author.toPullRequestMember(),
                reviewers.map(RealmPullRequestMember::toPullRequestMember).toList(),
                state, sourceBranch.toGitReference(), targetBranch.toGitReference())
    }

    companion object Factory {
        fun from(pullRequest: PullRequest): RealmPullRequest {
            val reviewers = RealmList<RealmPullRequestMember>()
            reviewers.addAll(pullRequest.reviewers.map(RealmPullRequestMember.Factory::from))
            return RealmPullRequest(pullRequest.id, pullRequest.title, pullRequest.createdDate,
                    pullRequest.updatedDate, RealmPullRequestMember.from(pullRequest.author),
                    reviewers, pullRequest.state, RealmGitReference.from(pullRequest.sourceBranch),
                    RealmGitReference.from(pullRequest.targetBranch))
        }
    }
}