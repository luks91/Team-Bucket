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

import android.support.annotation.StringRes
import com.github.luks91.teambucket.model.*
import io.reactivex.Observable

interface MainView : com.hannesdorfmann.mosby3.mvp.MvpView {
    fun requestUserCredentials(previousCredentials: BitbucketCredentials) : Observable<BitbucketCredentials>
    fun showErrorNotification(@StringRes message: Int)
    fun requestToSelectFrom(@android.support.annotation.StringRes titleRes: Int, resources: List<String>,
                            selectedIndices: IntArray): io.reactivex.Observable<List<Int>>
    fun intentRepositorySettings(): Observable<Any>
}