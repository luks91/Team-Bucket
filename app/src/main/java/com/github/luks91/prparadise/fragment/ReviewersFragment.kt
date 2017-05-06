/**
 * Copyright (c) 2017-present, PR Paradise Contributors.
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

package com.github.luks91.prparadise.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.luks91.prparadise.R
import com.github.luks91.prparadise.ReviewersView
import com.github.luks91.prparadise.model.Reviewer
import com.github.luks91.prparadise.presenter.ReviewersPresenter
import com.hannesdorfmann.mosby3.mvp.MvpFragment
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import com.github.luks91.prparadise.model.ReviewersInformation
import com.squareup.picasso.Picasso
import org.apache.commons.lang3.StringUtils

class ReviewersFragment : MvpFragment<ReviewersView, ReviewersPresenter>(), ReviewersView {

    val pullToRefreshSubject: PublishSubject<Any> = PublishSubject.create()
    val dataAdapter = ReviewersAdapter()

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
        val llm = LinearLayoutManager(context)
        recyclerView.layoutManager = llm
        recyclerView.adapter = dataAdapter
    }

    override fun onReviewersReceived(reviewers: ReviewersInformation) {
        dataAdapter.dataList.clear()
        dataAdapter.serverUrl = reviewers.serverUrl
        dataAdapter.dataList.addAll(reviewers.reviewers)
        dataAdapter.notifyDataSetChanged()
    }

    override fun intentPullToRefresh(): Observable<Any> {
        return pullToRefreshSubject
    }

    override fun onLoadingCompleted() {
        val swipeContainer = view!!.findViewById(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.isRefreshing = false
    }

    inner class ReviewersAdapter : RecyclerView.Adapter<ReviewersAdapter.DataViewHolder>() {

        val dataList = mutableListOf<Reviewer>()
        var serverUrl: String = StringUtils.EMPTY

        inner class DataViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var reviewerName: TextView = itemView.findViewById(R.id.reviewerName) as TextView
            internal var pullRequestsCount: TextView = itemView.findViewById(R.id.pullRequestsCount) as TextView
            internal var reviewerAvatar: ImageView = itemView.findViewById(R.id.reviewerAvatar) as ImageView
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): DataViewHolder {
            val v = LayoutInflater.from(parent!!.context).inflate(R.layout.reviewer_card, parent, false)
            return DataViewHolder(v)
        }

        override fun onBindViewHolder(holder: DataViewHolder?, position: Int) {
            val dataObject = dataList[position]
            holder!!.reviewerName.text = dataObject.user.displayName
            holder.pullRequestsCount.text = context.getString(R.string.reviewing_count, dataObject.reviewsCount)

            //TODO: Picasso interaction to be moved to the presenter
            Picasso.with(context)
                    .load(Uri.parse(serverUrl).buildUpon().appendEncodedPath(dataObject.user.avatarUrlSuffix).build())
                    .placeholder(R.drawable.ic_sentiment_satisfied_black_24dp)
                    .error(R.drawable.ic_sentiment_very_satisfied_black_24dp)
                    .into(holder.reviewerAvatar)
        }

        override fun getItemCount(): Int {
            return dataList.size
        }
    }
}