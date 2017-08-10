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

package com.github.luks91.teambucket

import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observable
import javax.inject.Inject

class ReactiveBus @Inject constructor(){
    private val relay: Relay<Any> = PublishRelay.create<Any>().toSerialized()

    fun post(event: Any) {
        relay.accept(event)
    }

    fun <T> receive(clazz: Class<T>): Observable<T> {
        return relay.ofType(clazz)
    }

    data class EventCredentialsInvalid(val notifier: String)
    data class EventRepositoriesMissing(val notifier: String)
    data class EventNoNetworkConnection(val notifier: String)

}