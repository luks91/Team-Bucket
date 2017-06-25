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

package com.github.luks91.prparadise.adapter

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.github.luks91.prparadise.R
import com.github.luks91.prparadise.model.PullRequest
import com.github.luks91.prparadise.model.Reviewer
import com.github.luks91.prparadise.model.ReviewersInformation
import com.github.luks91.prparadise.model.User
import com.squareup.picasso.Target
import org.apache.commons.lang3.StringUtils

class ReviewersAdapter(private val context: Context, private val callback: Callback,
                       private val layoutManager: LinearLayoutManager)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object Const {
        private const val REVIEWER = 0
        private const val PULL_REQUEST = 1
    }

    private val reviewersList = mutableListOf<Reviewer>()
    private val pullRequestsList = mutableListOf<PullRequest>()
    private var expandedReviewerIndex = Int.MAX_VALUE
    private var selectedReviewer: Reviewer? = null
    private var serverUrl: String = StringUtils.EMPTY

    inner class DataViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var reviewerName: TextView = itemView.findViewById(R.id.reviewerName) as TextView
        internal var pullRequestsCount: TextView = itemView.findViewById(R.id.pullRequestsCount) as TextView
        internal var reviewerAvatar: ImageView = itemView.findViewById(R.id.reviewerAvatar) as ImageView
        internal var expandArrow: View = itemView.findViewById(R.id.expandReviewerInfo)

        fun fillIn(reviewer: Reviewer, fillIndex: Int) {
            this.reviewerName.text = reviewer.user.displayName
            this.pullRequestsCount.text = context.getString(R.string.reviewing_count, reviewer.reviewsCount)

            expandArrow.rotation = if (expandedReviewerIndex == fillIndex) 180f else 0f

            itemView.setOnClickListener {
                val pullRequestsCount = pullRequestsList.size
                val index = reviewersList.indexOf(reviewer)

                if (expandedReviewerIndex == index && pullRequestsCount > 0) {
                    expandArrow.animate().rotation(0f).setDuration(300).start()
                    pullRequestsList.clear()
                    notifyItemRangeRemoved(expandedReviewerIndex + 1, pullRequestsCount)
                    expandedReviewerIndex = Int.MAX_VALUE
                    selectedReviewer = null
                } else if (expandedReviewerIndex != index) {
                    if (expandedReviewerIndex != Int.MAX_VALUE) {
                        notifyItemChanged(expandedReviewerIndex, reviewer)
                        notifyItemRangeRemoved(expandedReviewerIndex + 1, pullRequestsCount)
                        pullRequestsList.clear()
                    }

                    expandArrow.animate().rotation(180f).setDuration(300).start()
                    expandedReviewerIndex = index
                    selectedReviewer = reviewer
                    callback.retrieveReviewsFor(reviewer.user, index)
                }
            }

            val viewTarget = ImageViewTarget(reviewerAvatar)
            reviewerAvatar.tag = viewTarget //Picasso holds a WeakReference to the target, we need to strongly hold it here
            callback.loadImageFor(serverUrl, reviewer.user.avatarUrlSuffix, viewTarget)
        }

        fun updateItemSelection(reviewer: Reviewer, selectedReviewer: Reviewer) {
            if (selectedReviewer != reviewer) {
                expandArrow.animate().rotation(0f).setDuration(300).start()
            }
        }
    }

    inner class PullRequestViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var authorName: TextView = itemView.findViewById(R.id.reviewAuthor) as TextView
        internal var authorAvatar: ImageView = itemView.findViewById(R.id.authorAvatar) as ImageView

        internal var reviewers: Array<ImageView> = arrayOf(itemView.findViewById(R.id.firstReviewer) as ImageView,
                itemView.findViewById(R.id.secondReviewer) as ImageView,
                itemView.findViewById(R.id.thirdReviewer) as ImageView,
                itemView.findViewById(R.id.fourthReviewer) as ImageView)

        internal var reviewTitle: TextView = itemView.findViewById(R.id.reviewTitle) as TextView
        internal var reviewBranch: TextView = itemView.findViewById(R.id.reviewBranch) as TextView
        internal var targetBranch: TextView = itemView.findViewById(R.id.targetBranch) as TextView

        fun fillIn(pullRequest: PullRequest) {
            authorName.text = pullRequest.author.user.displayName
            reviewTitle.text = pullRequest.title
            reviewBranch.text = pullRequest.sourceBranch.displayId
            targetBranch.text = pullRequest.targetBranch.displayId

            callback.loadImageFor(serverUrl, pullRequest.author.user.avatarUrlSuffix, ImageViewTarget(authorAvatar))

            val pullRequestReviewers = pullRequest.reviewers
            for ((index, reviewerView) in reviewers.withIndex()) {
                if (pullRequestReviewers.size > index) {
                    callback.loadImageFor(serverUrl, pullRequestReviewers[index].user.avatarUrlSuffix,
                            ImageViewTarget(reviewerView))
                    reviewerView.visibility = View.VISIBLE
                } else {
                    reviewerView.visibility = View.GONE
                }
            }
        }
    }

    fun onReviewersReceived(reviewers: ReviewersInformation) {
        reviewersList.clear()
        pullRequestsList.clear()
        expandedReviewerIndex = Int.MAX_VALUE
        serverUrl = reviewers.serverUrl
        reviewersList.addAll(reviewers.reviewers)
        notifyDataSetChanged()
    }

    fun onPullRequestsProvided(reviews: List<IndexedValue<PullRequest>>) {
        layoutManager.scrollToPositionWithOffset(expandedReviewerIndex, 0)
        pullRequestsList.clear()
        for ((index, value) in reviews) {
            pullRequestsList.add(value)
            notifyItemInserted(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent!!.context)
        when(viewType) {
            PULL_REQUEST -> return PullRequestViewHolder(inflater.inflate(R.layout.pull_request_card, parent, false))
            else -> return DataViewHolder(inflater.inflate(R.layout.reviewer_card, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position > expandedReviewerIndex && position <= expandedReviewerIndex + pullRequestsList.size) {
            return PULL_REQUEST
        } else {
            return REVIEWER
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, index: Int) {
        when(holder) {
            is DataViewHolder -> holder.fillIn(
                    reviewersList[if (index <= expandedReviewerIndex) index else index - pullRequestsList.size], index)
            is PullRequestViewHolder -> holder.fillIn(pullRequestsList[index - expandedReviewerIndex - 1])
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, index: Int, payloads: MutableList<Any>?) {
        if (payloads != null && !payloads.isEmpty()) {
            for (payload in payloads) {
                if (payload is Reviewer && holder is DataViewHolder) {
                    holder.updateItemSelection(
                            reviewersList[if (index <= expandedReviewerIndex) index else index - pullRequestsList.size],
                            payload)
                }
            }
        } else {
            super.onBindViewHolder(holder, index, payloads)
        }
    }

    override fun getItemCount(): Int {
        return reviewersList.size + pullRequestsList.size
    }

    interface Callback {
        fun retrieveReviewsFor(user: User, index: Int)
        fun loadImageFor(serverUrl: String, urlPath: String, target: Target)
    }
}