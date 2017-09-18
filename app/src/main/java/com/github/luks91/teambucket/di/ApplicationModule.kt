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
import android.net.ConnectivityManager
import com.github.luks91.teambucket.connection.ConnectionProvider
import com.github.luks91.teambucket.TeamMembersProvider
import com.github.luks91.teambucket.model.BitbucketCredentials
import com.github.luks91.teambucket.persistence.PullRequestsStorage
import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.connection.CredentialsValidator
import com.github.luks91.teambucket.main.MainActivityComponent
import com.github.luks91.teambucket.persistence.RepositoriesStorage
import com.github.luks91.teambucket.persistence.TeamMembersStorage
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module(subcomponents = arrayOf(MainActivityComponent::class))
class ApplicationModule {

    @Provides
    @Singleton
    @AppContext
    fun provideAppContext(application: Application): Context = application

    @Provides
    @Singleton
    @AppPreferences
    fun provideAppPreferences(application: Application): SharedPreferences =
         application.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun providePullRequestsStorage() = PullRequestsStorage()

    @Provides
    @Singleton
    fun provideTeamMembersStorage(@AppContext context: Context) = TeamMembersStorage(context)

    @Provides
    @Singleton
    fun provideRepositoriesStorage(eventsBus: ReactiveBus) = RepositoriesStorage(eventsBus)

    @Provides
    @Singleton
    fun provideConnectivityManager(@AppContext context: Context): ConnectivityManager =
         context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    @Singleton
    internal fun provideConnectionProvider(@AppContext context: Context, @AppPreferences preferences: SharedPreferences,
                                           credentialsAdapter: JsonAdapter<BitbucketCredentials>,
                                           eventsBus: ReactiveBus, credentialsValidator: CredentialsValidator,
                                           httpClient: OkHttpClient): ConnectionProvider =
         ConnectionProvider(context, preferences, credentialsAdapter, credentialsValidator, eventsBus, httpClient)

    @Provides
    @Singleton
    internal fun provideCredentialsValidator(connectivityManager: ConnectivityManager, eventsBus: ReactiveBus,
                                             httpClient: OkHttpClient): CredentialsValidator =
            CredentialsValidator(connectivityManager, eventsBus, httpClient)

    @Provides
    @Singleton
    fun provideTeamMembersProvider(connectionProvider: ConnectionProvider, teamMembersStorage: TeamMembersStorage,
                                   repositoriesStorage: RepositoriesStorage):
            TeamMembersProvider = TeamMembersProvider(connectionProvider, teamMembersStorage, repositoriesStorage)

    @Provides
    @Singleton
    fun provideCredentialsAdapter(): JsonAdapter<BitbucketCredentials> =
            Moshi.Builder().build().adapter<BitbucketCredentials>(BitbucketCredentials::class.java)

    @Provides
    @Singleton
    fun provideEventsBus(): ReactiveBus = ReactiveBus()
}