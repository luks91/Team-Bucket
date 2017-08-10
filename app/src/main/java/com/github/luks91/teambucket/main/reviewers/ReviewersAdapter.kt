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

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.main.base.PullRequestViewHolder
import com.github.luks91.teambucket.model.*
import com.github.luks91.teambucket.util.ImageViewTarget
import io.reactivex.functions.Consumer

class ReviewersAdapter(private val context: Context, private val avatarLoadRequests: Consumer<AvatarLoadRequest>,
                       private val pullRequestsLoadRequest: Consumer<IndexedValue<User>>,
                       private val layoutManager: LinearLayoutManager) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        private const val REVIEWER = 0
        private const val PULL_REQUEST = 1
        private const val EMPTY_VIEW = 2
    }

    private val reviewersList = mutableListOf<Reviewer>()
    private var leadReviewer: User? = null
    private val pullRequestsList = mutableListOf<PullRequest>()
    private var expandedReviewerIndex = Int.MAX_VALUE
    private var selectedReviewer: Reviewer? = null

    inner class ReviewersViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val reviewerName: TextView = itemView.findViewById(R.id.reviewerName) as TextView
        private val pullRequestsCount: TextView = itemView.findViewById(R.id.pullRequestsCount) as TextView
        private val reviewerAvatar: ImageView = itemView.findViewById(R.id.reviewerAvatar) as ImageView
        private val expandArrow: View = itemView.findViewById(R.id.expandReviewerInfo)
        private val lazyReviewerWarning: View = itemView.findViewById(R.id.lazyReviewerWarning)

        fun fillIn(reviewer: Reviewer, fillIndex: Int) {
            this.reviewerName.text = reviewer.user.displayName
            this.pullRequestsCount.text = context.getString(
                    if (reviewer.user == leadReviewer) R.string.lead_reviewing_count else R.string.reviewing_count,
                    reviewer.reviewsCount)

            if (reviewer.reviewsCount == 0) {
                expandArrow.visibility = View.GONE
                itemView.setOnClickListener(null)
            } else {
                expandArrow.visibility = View.VISIBLE
                expandArrow.rotation = if (expandedReviewerIndex == fillIndex) 180f else 0f

                itemView.setOnClickListener {
                    val pullRequestsSize = pullRequestsList.size
                    val index = reviewersList.indexOf(reviewer)

                    if (expandedReviewerIndex == index && pullRequestsSize > 0) {
                        expandArrow.animate().rotation(0f).setDuration(300).start()
                        pullRequestsList.clear()
                        notifyItemRangeRemoved(expandedReviewerIndex + 1, pullRequestsSize)
                        expandedReviewerIndex = Int.MAX_VALUE
                        selectedReviewer = null
                    } else if (expandedReviewerIndex != index) {
                        if (expandedReviewerIndex != Int.MAX_VALUE) {
                            notifyItemChanged(expandedReviewerIndex, reviewer)
                            notifyItemRangeRemoved(expandedReviewerIndex + 1, pullRequestsSize)
                            pullRequestsList.clear()
                        }

                        expandArrow.animate().rotation(180f).setDuration(300).start()

                        expandedReviewerIndex = index
                        selectedReviewer = reviewer
                        pullRequestsLoadRequest.accept(IndexedValue(index, reviewer.user))
                    }
                }
            }

            lazyReviewerWarning.visibility = if (reviewer.isLazy) View.VISIBLE else View.GONE
            avatarLoadRequests.accept(AvatarLoadRequest(reviewer.user, ImageViewTarget(reviewerAvatar)))
        }

        fun updateItemSelection(reviewer: Reviewer, selectedReviewer: Reviewer) {
            if (selectedReviewer != reviewer) {
                expandArrow.animate().rotation(0f).setDuration(300).start()
            }
        }
    }

    fun onReviewersReceived(reviewers: ReviewersInformation) {
        reviewersList.clear()
        pullRequestsList.clear()
        expandedReviewerIndex = Int.MAX_VALUE
        leadReviewer = reviewers.lead
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
            EMPTY_VIEW -> return object: RecyclerView.ViewHolder(inflater.inflate(R.layout.no_data_view, parent, false)) {}
            PULL_REQUEST -> return PullRequestViewHolder(inflater.inflate(R.layout.pull_request_card, parent, false),
                    avatarLoadRequests)
            else -> return ReviewersViewHolder(inflater.inflate(R.layout.reviewer_card, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (reviewersList.isEmpty()) {
            return EMPTY_VIEW
        }

        if (position > expandedReviewerIndex && position <= expandedReviewerIndex + pullRequestsList.size) {
            return PULL_REQUEST
        } else {
            return REVIEWER
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, index: Int, payloads: MutableList<Any>?) {
        if (payloads != null && !payloads.isEmpty()) {
            for (payload in payloads) {
                if (payload is Reviewer && holder is ReviewersViewHolder) {
                    holder.updateItemSelection(
                            reviewersList[if (index <= expandedReviewerIndex) index else index - pullRequestsList.size], payload)
                }
            }
        } else {
            super.onBindViewHolder(holder, index, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, index: Int) {
        when(holder) {
            is ReviewersViewHolder -> holder.fillIn(
                    reviewersList[if (index <= expandedReviewerIndex) index else index - pullRequestsList.size], index)
            is PullRequestViewHolder -> holder.fillIn(pullRequestsList[index - expandedReviewerIndex - 1])
        }
    }

    override fun getItemCount(): Int {
        reviewersList.withIndex()

        return if (reviewersList.isEmpty()) 1 else reviewersList.size + pullRequestsList.size
    }
}