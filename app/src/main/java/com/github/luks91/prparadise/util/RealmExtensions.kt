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

package com.github.luks91.prparadise.util

import io.reactivex.*
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmResults

//as of May 29, 2017, Realm does not support RxJava2.0, so below, custom implementation is necessary
fun <T : RealmModel> RealmResults<T>.asFlowable(): Flowable<RealmResults<T>> {
    return Flowable.create({ emitter: FlowableEmitter<RealmResults<T>> ->
        emitter.onNext(this@asFlowable)
        val changeListener = RealmChangeListener<RealmResults<T>> { data -> emitter.onNext(data) }
        this@asFlowable.addChangeListener(changeListener)
        emitter.setCancellable { this@asFlowable.removeChangeListener(changeListener) }
    }, BackpressureStrategy.LATEST)
}
