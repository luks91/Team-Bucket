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

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Base64
import android.webkit.URLUtil
import com.facebook.android.crypto.keychain.AndroidConceal
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain
import com.facebook.crypto.Crypto
import com.facebook.crypto.CryptoConfig
import com.facebook.crypto.Entity
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.di.AppContext
import com.github.luks91.teambucket.di.AppPreferences
import com.github.luks91.teambucket.model.BitbucketConnection
import com.github.luks91.teambucket.model.BitbucketCredentials
import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.util.edit
import com.github.luks91.teambucket.util.put
import com.squareup.moshi.JsonAdapter
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import org.apache.commons.lang3.StringUtils
import java.nio.charset.Charset
import javax.inject.Inject

class ConnectionProvider @Inject internal constructor(@AppContext private val context: Context,
                                                      @AppPreferences private val preferences: SharedPreferences,
                                                      private val credentialsAdapter: JsonAdapter<BitbucketCredentials>,
                                                      private val credentialsValidator: CredentialsValidator,
                                                      private val eventsBus: ReactiveBus,
                                                      private val httpClient: OkHttpClient) {

    private val codingCharset = Charset.forName("UTF-8")
    private val credentialsEntity = "entity_"
    private val prefKey: String = "avatar_check_sum_2"
    private val notifyCredentialsInvalid = { eventsBus.post(ReactiveBus.EventCredentialsInvalid(javaClass.simpleName,
            R.string.toast_server_or_credentials_invalid)) }

    private val credentialsObservable =
            obtainSecurityCrypto().switchMap { crypto ->
                Observable.merge(
                        obtainDecryptedCredentials(crypto, credentialsAdapter)
                                .compose { credentialsValidator.neverIfInvalid(it, notifyCredentialsInvalid) },
                        eventsBus.receive(BitbucketCredentials::class.java)
                                .observeOn(Schedulers.io())
                                .compose { credentialsValidator.neverIfInvalid(it, notifyCredentialsInvalid) }
                                .switchMap { data ->
                                    if (URLUtil.isValidUrl(data.bitBucketUrl)) {
                                        Observable.just(data)
                                    } else {
                                        eventsBus.post(ReactiveBus.EventCredentialsInvalid(
                                                ConnectionProvider::class.java.simpleName, R.string.toast_server_url_invalid))
                                        Observable.never()
                                    }
                                }
                                .doOnNext { data -> preferences.edit { put(prefKey to encrypt(crypto, data)) } })
            }.subscribeOn(Schedulers.io()).replay(1).refCount()

    private fun obtainSecurityCrypto(): Observable<Crypto> {
        return Observable.defer {
            val keyChain = SharedPrefsBackedKeyChain(context, CryptoConfig.KEY_256)
            Observable.just(AndroidConceal.get().createDefaultCrypto(keyChain))
        }
    }

    private fun encrypt(crypto: Crypto, credentials: BitbucketCredentials): String {
        val jsonCredentials = credentialsAdapter.toJson(credentials)
        val encodedBytes = crypto.encrypt(jsonCredentials.toByteArray(codingCharset), Entity.create(credentialsEntity))
        return Base64.encodeToString(encodedBytes, Base64.DEFAULT)
    }

    private fun obtainDecryptedCredentials(crypto: Crypto, credentialsAdapter: JsonAdapter<BitbucketCredentials>):
            Observable<BitbucketCredentials> {
        val encryptedString = preferences.getString(prefKey, StringUtils.EMPTY)
        if (!encryptedString.isEmpty()) {
            val decodedBytes = crypto.decrypt(Base64.decode(encryptedString, Base64.DEFAULT), Entity.create(credentialsEntity))
            val credentials = credentialsAdapter.fromJson(decodedBytes.toString(codingCharset))
            if (!TextUtils.isEmpty(credentials.bitBucketUrl)) {
                return Observable.just(credentials)
            }
        }

        return Observable.empty()
    }

    fun connections(): Observable<BitbucketConnection> = credentialsObservable
            .map { credentials -> BitbucketConnection.from(credentials, httpClient) }

    fun cachedCredentials(): Single<BitbucketCredentials> = obtainSecurityCrypto()
            .switchMap { obtainDecryptedCredentials(it, credentialsAdapter) }
            .first(BitbucketCredentials.EMPTY)

    fun <TData> handleNetworkError(sender: String): ((t: Throwable) -> Observable<TData>) =
            credentialsValidator.handleNetworkError(sender)
}