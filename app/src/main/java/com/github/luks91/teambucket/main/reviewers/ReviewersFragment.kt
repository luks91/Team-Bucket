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

package com.github.luks91.teambucket.main.reviewers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.luks91.teambucket.R
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.github.luks91.teambucket.main.base.BasePullRequestsFragment
import com.github.luks91.teambucket.model.*
import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import javax.inject.Inject

class ReviewersFragment : BasePullRequestsFragment<ReviewersView, ReviewersPresenter>(), ReviewersView {

    private val reviewsForUserSubject: Relay<IndexedValue<User>> = PublishRelay.create()
    private val imageLoadRequests: Relay<AvatarLoadRequest> = PublishRelay.create()
    private val layoutManager by lazy { LinearLayoutManager(context) }
    private val dataAdapter by lazy { ReviewersAdapter(context, imageLoadRequests, reviewsForUserSubject, layoutManager) }

    @Inject lateinit var reviewersPresenter: ReviewersPresenter

    companion object Factory {
        fun newInstance() = ReviewersFragment()
    }

    override fun createPresenter() = reviewersPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_swipe_recycler_view, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view!!.findViewById(R.id.reviewersRecyclerView) as RecyclerView
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = dataAdapter
    }

    override fun onReviewersReceived(reviewers: ReviewersInformation) = dataAdapter.onReviewersReceived(reviewers)

    override fun intentRetrieveReviews() = reviewsForUserSubject

    override fun onPullRequestsProvided(reviews: List<IndexedValue<PullRequest>>) = dataAdapter.onPullRequestsProvided(reviews)

    override fun intentLoadAvatarImage() = imageLoadRequests
}