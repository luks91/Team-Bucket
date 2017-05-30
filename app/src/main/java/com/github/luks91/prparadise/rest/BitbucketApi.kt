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

package com.github.luks91.prparadise.rest

import com.github.luks91.prparadise.model.*
import com.github.luks91.prparadise.util.ReactiveBus
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.functions.BiConsumer
import io.reactivex.subjects.BehaviorSubject
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.Callable

interface BitbucketApi {

    @GET("/rest/api/1.0/projects/{projectName}/repos/{slug}/pull-requests")
    fun getPullRequests(
            @Header("Authorization") token: String,
            @Path("projectName") projectName: String,
            @Path("slug") slug: String,
            @Query("start") start: Int,
            @Query("limit") limit: Int = BitbucketApi.PAGE_SIZE,
            @Query("avatarSize") avatarSize: Int = 92
    ): Observable<PagedResponse<PullRequest>>

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

    companion object Utility {
        val PAGE_SIZE = 50

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

        fun <TData> handleNetworkError(sender: String): ((t: Throwable) -> Observable<TData>) {
            return { t: Throwable ->
                when (t) {
                    is HttpException -> {
                        if (t.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                            ReactiveBus.INSTANCE.post(ReactiveBus.EventCredentialsInvalid(sender))
                            Observable.empty<TData>()
                        } else {
                            Observable.error(t)
                        }
                    }
                    is SocketException -> {
                        ReactiveBus.INSTANCE.post(ReactiveBus.EventNoNetworkConnection(sender))
                        Observable.empty<TData>()
                    }
                    is UnknownHostException -> {
                        ReactiveBus.INSTANCE.post(ReactiveBus.EventNoNetworkConnection(sender))
                        Observable.empty<TData>()
                    }
                    is InterruptedIOException -> {
                        Observable.empty<TData>()
                    }
                    else -> Observable.error(t)
                }
            }
        }
    }
}