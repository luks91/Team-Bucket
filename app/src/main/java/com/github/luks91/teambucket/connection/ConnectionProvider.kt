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

import android.annotation.SuppressLint
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
import org.apache.commons.lang3.StringUtils
import java.nio.charset.Charset
import javax.inject.Inject

class ConnectionProvider @Inject constructor(@AppContext private val context: Context,
                                             @AppPreferences private val preferences: SharedPreferences,
                                             private val credentialsAdapter: JsonAdapter<BitbucketCredentials>,
                                             private val eventsBus: ReactiveBus) {

    private val codingCharset = Charset.forName("UTF-8")
    private val credentialsEntity = "entity_"
    private val prefKey: String = "avatar_check_sum_2"

    @SuppressLint("ApplySharedPref") //Commit is executed on a background thread.
    fun connections(): Observable<BitbucketConnection> {
        return obtainSecurityCrypto().switchMap { crypto ->
            Observable.merge(
                    obtainDecryptedCredentials(crypto, credentialsAdapter),
                    eventsBus.receive(BitbucketCredentials::class.java)
                            .switchMap { data ->
                                if (URLUtil.isValidUrl(data.bitBucketUrl)) {
                                    Observable.just(data)
                                } else {
                                    eventsBus.post(ReactiveBus.EventCredentialsInvalid(
                                            this@ConnectionProvider::class.java.simpleName, R.string.toast_server_url_invalid))
                                    Observable.never()
                                }
                            }
                            .doOnNext { data -> preferences.edit { put(prefKey to encrypt(crypto, data)) } })
        }.map { credentials -> BitbucketConnection.from(credentials) }
    }

    fun cachedCredentials(): Single<BitbucketCredentials> {
        return obtainSecurityCrypto()
                .switchMap { obtainDecryptedCredentials(it, credentialsAdapter, false) }
                .first(BitbucketCredentials.EMPTY)
    }

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

    private fun obtainDecryptedCredentials(crypto: Crypto, credentialsAdapter: JsonAdapter<BitbucketCredentials>,
                                           notifyError: Boolean = true)
            : Observable<BitbucketCredentials> {
        val encryptedString = preferences.getString(prefKey, StringUtils.EMPTY)
        if (!encryptedString.isEmpty()) {
            val decodedBytes = crypto.decrypt(Base64.decode(encryptedString, Base64.DEFAULT), Entity.create(credentialsEntity))
            val credentials = credentialsAdapter.fromJson(decodedBytes.toString(codingCharset))
            if (!TextUtils.isEmpty(credentials.bitBucketUrl)) {
                return Observable.just(credentials)
            }
        }

        return Observable.empty<BitbucketCredentials>().doOnSubscribe {
            if (notifyError) {
                eventsBus.post(ReactiveBus.EventCredentialsInvalid(javaClass.simpleName))
            }
        }
    }
}