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

package com.github.luks91.teambucket.connection

import com.github.luks91.teambucket.model.*
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.functions.BiConsumer
import io.reactivex.subjects.BehaviorSubject
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.Callable

interface BitbucketApi {

    @GET("/rest/api/1.0/projects/{projectName}/repos/{slug}/pull-requests")
    fun getPullRequests(
            @Header("Authorization") token: String,
            @Path("projectName") projectName: String,
            @Path("slug") slug: String,
            @Query("start") start: Int,
            @Query("limit") limit: Int = BitbucketApi.PAGE_SIZE,
            @PullRequestStatus @Query("state") status: String = STATUS_OPEN,
            @Query("avatarSize") avatarSize: Int = 92,
            @Order @Query("order") order: String = NEWEST
    ): Observable<PagedResponse<PullRequest>>

    @GET("/rest/api/1.0/projects/{projectName}/repos/{slug}/pull-requests/{id}/activities")
    fun getPullRequestActivities(
            @Header("Authorization") token: String,
            @Path("projectName") projectName: String,
            @Path("slug") slug: String,
            @Path("id") pullRequestId: Long,
            @Query("start") start: Int,
            @Query("limit") limit: Int = BitbucketApi.PAGE_SIZE
    ): Observable<PagedResponse<PullRequestActivity>>

    @GET("/rest/api/1.0/projects/{projectName}/repos/{slug}/participants")
    fun getRepositoryParticipants(
            @Header("Authorization") token: String,
            @Path("projectName") projectName: String,
            @Path("slug") slug: String,
            @Query("start") start: Int,
            @Query("limit") limit: Int = BitbucketApi.PAGE_SIZE
    ): Observable<PagedResponse<User>>

    @GET("/rest/api/1.0/projects/{projectName}/repos")
    fun getProjectRepositories(
            @Header("Authorization") token: String,
            @Path("projectName") projectName: String,
            @Query("start") start: Int,
            @Query("limit") limit: Int = BitbucketApi.PAGE_SIZE
    ): Observable<PagedResponse<Repository>>

    @GET("/rest/api/1.0/projects/")
    fun getProjects(
            @Header("Authorization") token: String,
            @Query("start") start: Int,
            @Query("limit") limit: Int = BitbucketApi.PAGE_SIZE
    ): Observable<PagedResponse<Project>>

    @GET("/rest/api/1.0/users/{userName}")
    fun getUser(
            @Header("Authorization") token: String,
            @Path("userName") userName: String
    ): Observable<User>

    companion object Utility {
        const val PAGE_SIZE = 50

        fun <TData> queryPaged(generator: (start: Int) -> Observable<PagedResponse<TData>>): Observable<List<TData>> {
            return Observable.generate<List<TData>, BehaviorSubject<Int>>(
                    Callable<BehaviorSubject<Int>> { BehaviorSubject.createDefault(0) },
                    BiConsumer<BehaviorSubject<Int>, Emitter<List<TData>>> { pageSubject, emitter ->
                        pageSubject.take(1)
                                .concatMap { start -> generator.invoke(start) }
                                .blockingSubscribe({ data ->
                                    if (data.isLastPage) {
                                        if (!data.values.isEmpty()) {
                                            emitter.onNext(data.values)
                                        }
                                        emitter.onComplete()
                                    } else {
                                        emitter.onNext(data.values)
                                        pageSubject.onNext(data.nextPageStart)
                                    }
                                }, { error -> emitter.onError(error) })
                    })
        }
    }
}