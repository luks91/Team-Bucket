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

package com.github.luks91.teambucket.main

import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.connection.ConnectionProvider
import com.github.luks91.teambucket.main.reviewers.ReviewersFragmentComponent
import com.github.luks91.teambucket.persistence.PersistenceProvider
import dagger.Module
import dagger.Provides

@Module(subcomponents = arrayOf(ReviewersFragmentComponent::class))
class MainActivityModule {

    @Provides
    internal fun provideMainView(mainActivity: MainActivity): MainView {
        return mainActivity
    }

    @Provides
    internal fun provideMainPresenter(connectionProvider: ConnectionProvider, persistenceProvider: PersistenceProvider,
                                      eventBus: ReactiveBus): MainPresenter {
        return MainPresenter(connectionProvider, persistenceProvider, eventBus)
    }
}