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

package com.github.luks91.prparadise.model

import android.util.Base64
import com.github.luks91.prparadise.rest.BitbucketApi
import com.squareup.moshi.Json
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.nio.charset.Charset

val EMPTY_STRING = ""

data class BitbucketCredentials(val bitBucketUrl: String, val username: String, val password: String)

data class BitbucketConnection(val serverUrl: String, val api: BitbucketApi, val token: String) {
    companion object Factory {
        fun from(credentials: BitbucketCredentials): BitbucketConnection {
            return BitbucketConnection(credentials.bitBucketUrl, createBitbucketApi(credentials.bitBucketUrl),
                    createBasicToken(credentials.username, credentials.password))
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
                  @Json(name = "state") val state: String)

data class Reviewer(val user: User, val reviewsCount: Int)
data class ReviewersInformation(val reviewers: List<Reviewer>, val serverUrl: String)

data class PagedResponse<out T>(@Json(name = "size") val size: Int,
                                @Json(name = "limit") val limit: Int,
                                @Json(name = "isLastPage") val isLastPage: Boolean,
                                @Json(name = "values") val values: List<T>,
                                @Json(name = "start") val start: Int,
                                @Json(name = "nextPageStart") val nextPageStart: Int = Int.MAX_VALUE)