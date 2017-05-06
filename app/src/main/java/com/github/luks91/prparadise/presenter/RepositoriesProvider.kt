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

import com.github.luks91.prparadise.model.Repository
import com.github.luks91.prparadise.util.ReactiveBus
import io.reactivex.Observable

internal class RepositoriesProvider {

    fun obtainSelectedRepositories(): Observable<List<Repository>> {
        return ReactiveBus.INSTANCE.receive(ReactiveBus.EventRepositories::class.java)
                .map { event -> event.repositories }
                .doOnSubscribe {
                    ReactiveBus.INSTANCE.post(ReactiveBus.EventRepositoriesMissing(RepositoriesProvider::class.java.simpleName))
                }
    }

}
