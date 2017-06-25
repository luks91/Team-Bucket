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

package com.github.luks91.prparadise.presenter

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.facebook.android.crypto.keychain.AndroidConceal
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain
import com.facebook.crypto.Crypto
import com.facebook.crypto.CryptoConfig
import com.facebook.crypto.Entity
import com.github.luks91.prparadise.model.BitbucketConnection
import com.github.luks91.prparadise.model.BitbucketCredentials
import com.github.luks91.prparadise.util.ReactiveBus
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import org.apache.commons.lang3.StringUtils
import java.nio.charset.Charset

internal class ConnectionProvider(private val context: Context, private val prefs: SharedPreferences) {

    private val codingCharset = Charset.forName("UTF-8")
    private val credentialsEntity = "entity_"
    private val prefKey: String = "avatar_check_sum"
    private val credentialsAdapter = Moshi.Builder().build().adapter<BitbucketCredentials>(BitbucketCredentials::class.java)!!

    fun obtainConnection(): Observable<BitbucketConnection> {
        return obtainSecurityCrypto().flatMap { crypto ->
            Observable.merge(
                    obtainDecryptedCredentials(crypto, credentialsAdapter),
                    ReactiveBus.INSTANCE.receive(BitbucketCredentials::class.java)
                            .doOnNext { data -> prefs.edit().putString(prefKey, encrypt(crypto, data)).commit() })
        }.map { credentials -> BitbucketConnection.from(credentials) }
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

    private fun obtainDecryptedCredentials(crypto: Crypto, credentialsAdapter: JsonAdapter<BitbucketCredentials>)
            : Observable<BitbucketCredentials> {
        val encryptedString = prefs.getString(prefKey, StringUtils.EMPTY)
        if (!encryptedString.isEmpty()) {
            val decodedBytes = crypto.decrypt(Base64.decode(encryptedString, Base64.DEFAULT), Entity.create(credentialsEntity))
            val credentials = credentialsAdapter.fromJson(decodedBytes.toString(codingCharset))
            return Observable.just(credentials)
        } else {
            return Observable.empty<BitbucketCredentials>().doOnSubscribe {
                ReactiveBus.INSTANCE.post(ReactiveBus.EventCredentialsInvalid(javaClass.simpleName))
            }
        }
    }
}
