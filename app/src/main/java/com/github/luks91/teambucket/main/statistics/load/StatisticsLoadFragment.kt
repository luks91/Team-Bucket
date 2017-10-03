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

package com.github.luks91.teambucket.main.statistics.load

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.github.luks91.teambucket.R
import com.hannesdorfmann.mosby3.mvp.MvpFragment
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class StatisticsLoadFragment : MvpFragment<StatisticsLoadView, StatisticsLoadPresenter>(), StatisticsLoadView {
    @Inject lateinit var statisticsPresenter: StatisticsLoadPresenter
    private val intentPullToRefresh = PublishSubject.create<Any>()
    private val loadProgressBar: ProgressBar by lazy { view!!.findViewById(R.id.statisticsLoadProgressBar) as ProgressBar }
    private val progressText: TextView by lazy { view!!.findViewById(R.id.progressValue) as TextView }

    companion object Factory {
        fun newInstance() = StatisticsLoadFragment()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun createPresenter() = statisticsPresenter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_load_statistics, container, false)
    }

    override fun intentRefreshData(): Observable<Any> = intentPullToRefresh

    private var pullRequestsCount = 0
    private var completedPullRequestsCount = 0


    override fun onLoadingStarted() {
        loadProgressBar.visibility = View.VISIBLE
        completedPullRequestsCount = 0
        pullRequestsCount = 0
    }

    override fun onPullRequestDetected() {
        if (!isDetached) {
            activity.runOnUiThread {
                pullRequestsCount++
                updateProgress()
            }
        }
    }

    private fun updateProgress() {
        loadProgressBar.progress = if (pullRequestsCount == 0) 0
            else (100.0 * completedPullRequestsCount / pullRequestsCount).toInt()
        progressText.text = "$completedPullRequestsCount/$pullRequestsCount"
    }

    override fun onPullRequestProcessed() {
        if (!isDetached) {
            activity.runOnUiThread {
                completedPullRequestsCount++
                updateProgress()
            }
        }
    }

    override fun onLoadingCompleted() {
    }
}