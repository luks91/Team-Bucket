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

package com.github.luks91.teambucket.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.github.luks91.teambucket.connection.ConnectionProvider
import com.github.luks91.teambucket.TeamMembersProvider
import com.github.luks91.teambucket.model.BitbucketCredentials
import com.github.luks91.teambucket.persistence.PersistenceProvider
import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.main.MainActivityComponent
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(subcomponents = arrayOf(MainActivityComponent::class))
class ApplicationModule {

    @Provides
    @Singleton
    @AppContext
    fun provideAppContext(application: Application): Context {
        return application
    }

    @Provides
    @Singleton
    @AppPreferences
    fun provideAppPreferences(application: Application): SharedPreferences {
        return application.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun providePersistenceProvider(@AppContext context: Context, eventsBus: ReactiveBus): PersistenceProvider {
        return PersistenceProvider(context, eventsBus)
    }

    @Provides
    @Singleton
    fun provideConnectionProvider(@AppContext context: Context, @AppPreferences preferences: SharedPreferences,
                                  credentialsAdapter: JsonAdapter<BitbucketCredentials>,
                                  eventsBus: ReactiveBus): ConnectionProvider {
        return ConnectionProvider(context, preferences, credentialsAdapter, eventsBus)
    }

    @Provides
    @Singleton
    fun provideTeamMembersProvider(connectionProvider: ConnectionProvider, persistenceProvider: PersistenceProvider,
                                   eventsBus: ReactiveBus): TeamMembersProvider {
        return TeamMembersProvider(connectionProvider, persistenceProvider, eventsBus)
    }

    @Provides
    @Singleton
    fun provideCredentialsAdapter(): JsonAdapter<BitbucketCredentials> {
        return Moshi.Builder().build().adapter<BitbucketCredentials>(BitbucketCredentials::class.java)!!
    }

    @Provides
    @Singleton
    fun provideEventsBus(): ReactiveBus {
        return ReactiveBus()
    }
}