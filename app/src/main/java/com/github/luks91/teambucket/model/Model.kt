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
import com.github.luks91.teambucket.rest.BitbucketApi
import com.squareup.moshi.Json
import com.squareup.picasso.Target
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.nio.charset.Charset

val EMPTY_STRING = ""

data class BitbucketCredentials(val bitBucketUrl: String, val username: String, val password: String)

data class BitbucketConnection(val userName: String, val serverUrl: String, val api: BitbucketApi, val token: String) {
    companion object Factory {
        fun from(credentials: BitbucketCredentials): BitbucketConnection {
            return BitbucketConnection(credentials.username, credentials.bitBucketUrl,
                    createBitbucketApi(credentials.bitBucketUrl), createBasicToken(credentials.username, credentials.password))
        }

        private fun createBasicToken(username: String, password: String): String {
            val toBase64: String = username + ':' + password
            return "Basic " + Base64.encodeToString(toBase64.toByteArray(Charset.forName("UTF-8")), Base64.NO_WRAP)
        }

        private fun createBitbucketApi(url: String): BitbucketApi {
            return Retrofit.Builder()
                    .baseUrl(url)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
                    .create(BitbucketApi::class.java)
        }
    }
}

data class Project(@Json(name = "key") val key: String,
                   @Json(name = "name") val name: String,
                   @Json(name = "description") val description: String)

data class Repository(@Json(name = "slug") val slug: String,
                      @Json(name = "name") val name: String,
                      @Json(name = "project") val project: Project)

data class User(@Json(name = "id") val id: Int,
                @Json(name = "name") val name: String,
                @Json(name = "displayName") val displayName: String,
                @Json(name = "slug") val slug: String,
                @Json(name = "avatarUrl") val avatarUrlSuffix: String)

data class PullRequestMember(@Json(name = "user") val user: User,
                             @Json(name = "role") val role: String,
                             @Json(name = "approved") val approved: Boolean,
                             @Json(name = "status") val status: String)

data class PullRequest(@Json(name = "id") val id: Long,
                       @Json(name = "title") val title: String,
                       @Json(name = "createdDate") val createdDate: Long,
                       @Json(name = "updatedDate") val updatedDate: Long,
                       @Json(name = "author") val author: PullRequestMember,
                       @Json(name = "reviewers") val reviewers: List<PullRequestMember>,
                       @PullRequestStatus @Json(name = "state") val state: String,
                       @Json(name = "fromRef") val sourceBranch: GitReference,
                       @Json(name = "toRef") val targetBranch: GitReference)

@Retention(AnnotationRetention.SOURCE)
@StringDef(STATUS_OPEN, STATUS_MERGED, STATUS_ALL)
annotation class PullRequestStatus

const val STATUS_OPEN = "open"
const val STATUS_MERGED = "merged"
const val STATUS_ALL = "all"

data class GitReference(@Json(name = "displayId") val displayId: String,
                        @Json(name = "latestCommit") val latestCommit: String)

data class Reviewer(val user: User, val reviewsCount: Int)
data class ReviewersInformation(val reviewers: List<Reviewer>, val serverUrl: String)

data class PagedResponse<out T>(@Json(name = "size") val size: Int,
                                @Json(name = "limit") val limit: Int,
                                @Json(name = "isLastPage") val isLastPage: Boolean,
                                @Json(name = "values") val values: List<T>,
                                @Json(name = "start") val start: Int,
                                @Json(name = "nextPageStart") val nextPageStart: Int = Int.MAX_VALUE)

data class ImageLoadRequest(val serverUrl: String, val urlPath: String, val target: Target)