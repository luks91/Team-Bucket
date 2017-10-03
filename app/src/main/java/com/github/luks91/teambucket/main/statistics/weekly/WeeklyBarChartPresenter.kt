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

package com.github.luks91.teambucket.main.statistics.weekly

import com.github.luks91.teambucket.main.statistics.StatisticsStorage
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import io.reactivex.disposables.Disposables
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WeeklyBarChartPresenter @Inject constructor(private val statisticsStorage: StatisticsStorage):
        MvpPresenter<WeeklyBarChartView> {

    private var disposable = Disposables.empty()

    override fun attachView(view: WeeklyBarChartView) {
        disposable = statisticsStorage.statisticsFrom(7, TimeUnit.DAYS)
                .subscribe()
    }

    override fun detachView(retainInstance: Boolean) {
        disposable.dispose()
    }
}