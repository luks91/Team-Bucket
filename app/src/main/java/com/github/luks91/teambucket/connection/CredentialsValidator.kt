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

import android.webkit.URLUtil
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.model.BitbucketConnection
import com.github.luks91.teambucket.model.BitbucketCredentials
import io.reactivex.Observable
import okhttp3.OkHttpClient
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException

internal class CredentialsValidator(private val eventsBus: ReactiveBus, private val okHttpClient: OkHttpClient) {

    fun neverIfInvalid(credentials: Observable<BitbucketCredentials>, onInvalid: () -> Unit): Observable<BitbucketCredentials> =
            credentials
                    .switchIfEmpty {
                        onInvalid()
                        Observable.never<BitbucketCredentials>()
                    }.switchMap { secrets ->
                        if (!URLUtil.isValidUrl(secrets.bitBucketUrl)) {
                            Observable.never<BitbucketCredentials>().doOnSubscribe { onInvalid() }
                        } else {
                            val (userName, _, api, token) = BitbucketConnection.from(secrets, okHttpClient)
                            api.getUser(token, userName)
                                    .map { _ -> secrets }
                                    .onErrorResumeNext { error: Throwable ->
                                        if (error is HttpException && error.code() == HttpURLConnection.HTTP_UNAUTHORIZED)
                                            Observable.never<BitbucketCredentials>().doOnSubscribe { onInvalid() }
                                        else
                                            Observable.just(secrets)
                                    }
                        }
                    }

    fun <TData> handleNetworkError(sender: String): ((t: Throwable) -> Observable<TData>) {
        return { t: Throwable ->
            when (t) {
                is HttpException -> {
                    if (t.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        eventsBus.post(ReactiveBus.EventCredentialsInvalid(sender, R.string.toast_credentials_expired))
                        Observable.empty<TData>()
                    } else {
                        eventsBus.post(ReactiveBus.EventCredentialsInvalid(sender, R.string.toast_server_error))
                        Observable.empty<TData>()
                    }
                }
                is SocketException -> {
                    eventsBus.post(ReactiveBus.EventNoNetworkConnection(sender))
                    Observable.empty<TData>()
                }
                is UnknownHostException -> {
                    eventsBus.post(ReactiveBus.EventNoNetworkConnection(sender))
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
