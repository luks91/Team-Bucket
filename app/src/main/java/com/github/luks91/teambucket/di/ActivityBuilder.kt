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

import com.github.luks91.teambucket.main.MainActivity
import dagger.Module
import com.github.luks91.teambucket.main.MainActivityComponent
import android.app.Activity
import dagger.android.AndroidInjector
import dagger.android.ActivityKey
import dagger.multibindings.IntoMap
import dagger.Binds

@Module
abstract class ActivityBuilder {

    @Binds
    @IntoMap
    @ActivityKey(MainActivity::class)
    internal abstract fun bindMainActivity(builder: MainActivityComponent.Builder): AndroidInjector.Factory<out Activity>

}