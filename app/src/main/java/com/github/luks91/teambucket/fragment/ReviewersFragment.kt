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

package com.github.luks91.teambucket.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.ReviewersView
import com.github.luks91.teambucket.presenter.ReviewersPresenter
import com.hannesdorfmann.mosby3.mvp.MvpFragment
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.github.luks91.teambucket.adapter.ReviewersAdapter
import com.github.luks91.teambucket.model.ImageLoadRequest
import com.github.luks91.teambucket.model.PullRequest
import com.github.luks91.teambucket.model.ReviewersInformation
import com.github.luks91.teambucket.model.User
import com.squareup.picasso.Target

class ReviewersFragment : MvpFragment<ReviewersView, ReviewersPresenter>(), ReviewersView, ReviewersAdapter.Callback {

    private val pullToRefreshSubject: PublishSubject<Any> = PublishSubject.create()
    private val reviewsForUserSubject: PublishSubject<IndexedValue<User>> = PublishSubject.create()
    private val imageLoadRequests: PublishSubject<ImageLoadRequest> = PublishSubject.create()
    private val layoutManager: LinearLayoutManager by lazy { LinearLayoutManager(context) }
    private val dataAdapter: ReviewersAdapter by lazy { ReviewersAdapter(context, this, layoutManager) }

    companion object Factory {
        fun newInstance() : ReviewersFragment = ReviewersFragment()
    }

    override fun createPresenter(): ReviewersPresenter {
        return ReviewersPresenter(activity as Context)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_reviewers, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPullToRefresh()
        setupRecyclerView()
    }

    fun setupPullToRefresh() {
        val swipeContainer = view!!.findViewById(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.setOnRefreshListener { pullToRefreshSubject.onNext(Object()) }
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light)
    }

    fun setupRecyclerView() {
        val recyclerView = view!!.findViewById(R.id.reviewersRecyclerView) as RecyclerView
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = dataAdapter
    }

    override fun intentPullToRefresh(): Observable<Any> {
        return pullToRefreshSubject
    }

    override fun onLoadingCompleted() {
        val swipeContainer = view!!.findViewById(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.isRefreshing = false
    }

    override fun onReviewersReceived(reviewers: ReviewersInformation) {
        dataAdapter.onReviewersReceived(reviewers)
    }

    override fun retrieveReviewsFor(user: User, index: Int) {
        reviewsForUserSubject.onNext(IndexedValue(index, user))
    }

    override fun intentRetrieveReviews(): Observable<IndexedValue<User>> {
        return reviewsForUserSubject
    }

    override fun onPullRequestsProvided(reviews: List<IndexedValue<PullRequest>>) {
        dataAdapter.onPullRequestsProvided(reviews)
    }

    override fun intentLoadAvatarImage(): Observable<ImageLoadRequest> {
        return imageLoadRequests
    }

    override fun loadImageFor(serverUrl: String, urlPath: String, target: Target) {
        imageLoadRequests.onNext(ImageLoadRequest(serverUrl, urlPath, target))
    }
}