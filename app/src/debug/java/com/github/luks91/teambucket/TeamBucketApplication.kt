/**
 * Copyright (c) 2017-present, Team Bucket Contributors.

 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.github.luks91.teambucket

import com.github.luks91.teambucket.di.DaggerApplicationComponent
import dagger.android.HasActivityInjector
import android.app.Activity
import com.facebook.stetho.Stetho
import com.uphyca.stetho_realm.RealmInspectorModulesProvider
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import io.reactivex.plugins.RxJavaPlugins
import io.realm.Realm
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InterruptedIOException
import javax.inject.Inject
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric



class TeamBucketApplication : android.app.Application(), HasActivityInjector {

    @Inject
    lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>
    override fun onCreate() {
        super.onCreate()

        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        DaggerApplicationComponent.builder()
                .application(this)
                .okHttpClient(OkHttpClient.Builder().addInterceptor(logging).build())
                .build().inject(this)

        Fabric.with(this, Crashlytics())
        Realm.init(this)
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build())

        RxJavaPlugins.setErrorHandler {
            if (it.cause is InterruptedIOException) {
                //ignore interruptions (OkHttp issue)
            } else {
                val currentThread = Thread.currentThread()
                currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, it)
            }
        }
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return activityDispatchingAndroidInjector
    }
}