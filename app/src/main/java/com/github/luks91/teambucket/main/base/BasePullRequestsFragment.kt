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

package com.github.luks91.teambucket.main.base

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import com.github.luks91.teambucket.R
import com.hannesdorfmann.mosby3.mvp.MvpFragment
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

abstract class BasePullRequestsFragment<V: BasePullRequestsView, P: BasePullRequestsPresenter<V>>: MvpFragment<V, P>(),
        BasePullRequestsView {

    private val pullToRefreshSubject: PublishSubject<Any> = PublishSubject.create()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val swipeContainer = view!!.findViewById(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.setOnRefreshListener { pullToRefreshSubject.onNext(Object()) }
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light)
    }

    final override fun intentPullToRefresh(): Observable<Any> {
        return pullToRefreshSubject
    }

    final override fun onLoadingCompleted() {
        val swipeContainer = view!!.findViewById(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.isRefreshing = false
    }

    final override fun onSelfLoadingStarted() {
        val swipeContainer = view!!.findViewById(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.isRefreshing = true
    }
}
