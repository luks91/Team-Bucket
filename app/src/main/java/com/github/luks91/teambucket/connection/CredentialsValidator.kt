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

import android.net.ConnectivityManager
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.model.BitbucketConnection
import com.github.luks91.teambucket.model.BitbucketCredentials
import io.reactivex.Observable
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException

internal class CredentialsValidator(val connectivityManager: ConnectivityManager, val eventsBus: ReactiveBus) {

    fun neverIfInvalid(credentials: Observable<BitbucketCredentials>, onInvalid: () -> Unit): Observable<BitbucketCredentials> =
            credentials
                    .switchIfEmpty {
                        onInvalid()
                        Observable.never<BitbucketCredentials>()
                    }.switchMap { credentials ->
                        val (userName, _, api, token) = BitbucketConnection.from(credentials)
                        api.getUser(token, userName)
                                .onErrorResumeNext { _: Throwable -> Observable.empty() }
                                .map { _ -> credentials }
                                .switchIfEmpty(Observable.never<BitbucketCredentials>().doOnSubscribe { onInvalid() })
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
                    if (connectivityManager.activeNetworkInfo?.isConnected ?: false) {
                        eventsBus.post(ReactiveBus.EventCredentialsInvalid(sender, R.string.toast_cannot_reach_server))
                    } else {
                        eventsBus.post(ReactiveBus.EventNoNetworkConnection(sender))
                    }
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
