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

package com.github.luks91.teambucket.model

import android.support.annotation.StringDef
import android.util.Base64
import com.github.luks91.teambucket.connection.BitbucketApi
import com.squareup.moshi.Json
import com.squareup.picasso.Target
import okhttp3.OkHttpClient
import org.apache.commons.lang3.StringUtils
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

val EMPTY_STRING = ""

data class BitbucketCredentials(@Json(name = "url") val bitBucketUrl: String,
                                @Json(name = "username") val username: String,
                                @Json(name = "password") val password: String) {
    companion object {
        val EMPTY = BitbucketCredentials(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY)
    }
}

data class BitbucketConnection private constructor(val userName: String, val serverUrl: String, val api: BitbucketApi,
                                                   val token: String) {
    companion object Factory {
        fun from(credentials: BitbucketCredentials, client: OkHttpClient): BitbucketConnection {
            return BitbucketConnection(credentials.username, credentials.bitBucketUrl,
                    createBitbucketApi(credentials.bitBucketUrl, client),
                    createBasicToken(credentials.username, credentials.password))
        }

        private fun createBasicToken(username: String, password: String): String {
            val toBase64: String = username + ':' + password
            return "Basic ${Base64.encodeToString(toBase64.toByteArray(Charset.forName("UTF-8")), Base64.NO_WRAP)}"
        }

        private fun createBitbucketApi(url: String, client: OkHttpClient): BitbucketApi {
            return Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
                    .create(BitbucketApi::class.java)
        }
    }
}

data class Project(@Json(name = "key") val key: String,
                   @Json(name = "name") val name: String,
                   @Json(name = "description") val description: String?)

data class Repository(@Json(name = "slug") val slug: String,
                      @Json(name = "name") val name: String,
                      @Json(name = "project") val project: Project)

data class User(@Json(name = "id") val id: Long,
                @Json(name = "name") val name: String,
                @Json(name = "displayName") val displayName: String,
                @Json(name = "slug") val slug: String,
                @Json(name = "avatarUrl") val avatarUrlSuffix: String)

data class Density(val inbound: Int, val outbound: Int)

data class PullRequestMember(@Json(name = "user") val user: User,
                             @Json(name = "role") val role: String,
                             @Json(name = "approved") val approved: Boolean,
                             @Json(name = "status") @ReviewerState val status: String)


@Retention(AnnotationRetention.SOURCE)
@StringDef(NEEDS_WORK, UNAPPROVED, APPROVED)
annotation class ReviewerState

const val NEEDS_WORK = "NEEDS_WORK"
const val UNAPPROVED = "UNAPPROVED"
const val APPROVED = "APPROVED"

data class PullRequest(@Json(name = "id") val id: Long,
                       @Json(name = "title") val title: String,
                       @Json(name = "createdDate") val createdDate: Long,
                       @Json(name = "updatedDate") val updatedDate: Long,
                       @Json(name = "author") val author: PullRequestMember,
                       @Json(name = "reviewers") val reviewers: List<PullRequestMember>,
                       @PullRequestStatus @Json(name = "state") val state: String,
                       @Json(name = "fromRef") val sourceBranch: GitReference,
                       @Json(name = "toRef") val targetBranch: GitReference) {

    //TODO: Business logic.. should not be in model.
    fun isLazilyReviewed(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (TimeUnit.MILLISECONDS.toDays(updatedDate - createdDate) <= 3) {
            TimeUnit.MILLISECONDS.toDays(currentTime - createdDate) >= 4
        } else {
            TimeUnit.MILLISECONDS.toDays(currentTime - updatedDate) >= 2
        }
    }
}

@Retention(AnnotationRetention.SOURCE)
@StringDef(STATUS_OPEN, STATUS_MERGED, STATUS_ALL)
annotation class PullRequestStatus

const val STATUS_OPEN = "open"
const val STATUS_MERGED = "merged"
const val STATUS_ALL = "all"

@Retention(AnnotationRetention.SOURCE)
@StringDef(NEWEST, OLDEST)
annotation class Order

const val NEWEST = "NEWEST"
const val OLDEST = "OLDEST"

data class PullRequestActivity(
        @Json(name = "id") val id: Long,
        @Json(name = "createdDate") val createdDate: Long,
        @Json(name = "user") val user: User,
        @ActivityType @Json(name = "action") val action: String
)

@Retention(AnnotationRetention.SOURCE)
@StringDef(ACTIVITY_COMMENTED, ACTIVITY_APPROVED, ACTIVITY_NEEDS_WORK, ACTIVITY_OPENED, ACTIVITY_MERGED, ACTIVITY_DECLINED)
annotation class ActivityType

const val ACTIVITY_COMMENTED = "COMMENTED"
const val ACTIVITY_NEEDS_WORK = "REVIEWED"
const val ACTIVITY_APPROVED = "APPROVED"
const val ACTIVITY_OPENED = "OPENED"
const val ACTIVITY_MERGED = "MERGED"
const val ACTIVITY_DECLINED = "DECLINED"

data class GitReference(@Json(name = "displayId") val displayId: String,
                        @Json(name = "latestCommit") val latestCommit: String,
                        @Json(name = "repository") val repository: Repository)

data class Reviewer(val user: User, val density: Density, val reviewsCount: Int, val isLazy: Boolean)
data class ReviewersInformation(val reviewers: List<Reviewer>, val preferredReviewers: List<Reviewer>, val lead: User?,
                                val serverUrl: String) {
    companion object {
        val EMPTY = ReviewersInformation(listOf(), listOf(), null, StringUtils.EMPTY)
    }
}

data class PagedResponse<out T>(@Json(name = "size") val size: Int,
                                @Json(name = "limit") val limit: Int,
                                @Json(name = "isLastPage") val isLastPage: Boolean,
                                @Json(name = "values") val values: List<T>,
                                @Json(name = "start") val start: Int,
                                @Json(name = "nextPageStart") val nextPageStart: Int = Int.MAX_VALUE)

data class AvatarLoadRequest(val user: User, val target: Target)