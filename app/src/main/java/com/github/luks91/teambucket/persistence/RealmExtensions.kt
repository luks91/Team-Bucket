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

package com.github.luks91.teambucket.persistence

import io.reactivex.*
import io.realm.*

//as of May 29, 2017, Realm does not support RxJava2.0, so below, custom implementation is necessary
internal fun <T : RealmModel> RealmResults<T>.asFlowable(): Flowable<RealmResults<T>> {
    return Flowable.create({ emitter: FlowableEmitter<RealmResults<T>> ->
        emitter.onNext(this@asFlowable)
        val changeListener = RealmChangeListener<RealmResults<T>> { data -> emitter.onNext(data) }
        this@asFlowable.addChangeListener(changeListener)
        emitter.setCancellable { if (this@asFlowable.isValid) this@asFlowable.removeChangeListener(changeListener) }
    }, BackpressureStrategy.LATEST)
}

internal fun <T> usingRealm(name: String, scheduler: Scheduler, function: (realm: Realm) -> Observable<T>): Observable<T> {
    return io.reactivex.Observable.using(
            { Realm.getInstance(RealmConfiguration.Builder().name(name).deleteRealmIfMigrationNeeded().build()) },
            { realm -> function.invoke(realm) },
            { realm -> realm.close() } )
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
}