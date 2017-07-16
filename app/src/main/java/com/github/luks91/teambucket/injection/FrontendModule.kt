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

package com.github.luks91.teambucket.injection

import android.content.Context
import com.github.luks91.teambucket.ConnectionProvider
import com.github.luks91.teambucket.TeamMembersProvider
import com.github.luks91.teambucket.persistence.PersistenceProvider
import com.github.luks91.teambucket.presenter.MainPresenter
import com.github.luks91.teambucket.presenter.ReviewersPresenter
import com.github.luks91.teambucket.util.ReactiveBus
import dagger.Module
import dagger.Provides

@Module
class FrontendModule(val context: Context) {

    @Provides
    @PerActivity
    fun provideContext(): Context {
        return context
    }

    @Provides
    @PerActivity
    fun provideReviewersPresenter(@AppContext context: Context, connectionProvider: ConnectionProvider,
                                  persistenceProvider: PersistenceProvider, teamMembersProvider: TeamMembersProvider,
                                  eventsBus: ReactiveBus): ReviewersPresenter {
        return ReviewersPresenter(context, connectionProvider, persistenceProvider, teamMembersProvider, eventsBus)
    }

    @Provides
    @PerActivity
    fun provideMainPresenter(connectionProvider: ConnectionProvider, persistenceProvider: PersistenceProvider,
                             eventsBus: ReactiveBus): MainPresenter {
        return MainPresenter(connectionProvider, persistenceProvider, eventsBus)
    }
}