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

package com.github.luks91.teambucket.main.home

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.main.base.PullRequestViewHolder
import com.github.luks91.teambucket.model.AvatarLoadRequest
import com.github.luks91.teambucket.model.PullRequest
import com.github.luks91.teambucket.model.ReviewersInformation
import com.github.luks91.teambucket.util.BounceInterpolator
import com.github.luks91.teambucket.util.ImageViewTarget
import com.github.luks91.teambucket.util.childImageView
import com.github.luks91.teambucket.util.childTextView
import io.reactivex.functions.Consumer

class HomeAdapter(private val context: Context, private val avatarRequestsConsumer: Consumer<AvatarLoadRequest>):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val EMPTY_VIEW_TYPE = 0
    val SUGGESTION_VIEW_TYPE = 1
    val HEADER_VIEW_TYPE = 2
    val PULL_REQUEST_VIEW_TYPE = 3

    private var reviewers: ReviewersInformation = ReviewersInformation.EMPTY
    private var pullRequests = listOf<PullRequest>()

    fun onUserPullRequestsReceived(pullRequests: List<PullRequest>) {
        this.pullRequests = pullRequests
    }

    fun onReviewersReceived(reviewers: ReviewersInformation) {
        this.reviewers = reviewers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent!!.context)
        when (viewType) {
            EMPTY_VIEW_TYPE -> return object: RecyclerView.ViewHolder(inflater.inflate(R.layout.no_data_view, parent, false)) {}
            SUGGESTION_VIEW_TYPE -> return SuggestedReviewersViewHolder(
                                            inflater.inflate(R.layout.suggested_reviewers_card, parent, false))
            HEADER_VIEW_TYPE -> return HeaderViewHolder(inflater.inflate(R.layout.header_card, parent, false))
            else -> return PullRequestViewHolder(inflater.inflate(R.layout.pull_request_card, parent, false),
                                            avatarRequestsConsumer)
        }
    }

    override fun getItemCount() = if (reviewers == ReviewersInformation.EMPTY) 0 else 2 + pullRequests.size

    override fun getItemViewType(position: Int): Int =
            when {
                reviewers.reviewers.isEmpty() -> EMPTY_VIEW_TYPE
                position == 0 -> SUGGESTION_VIEW_TYPE
                position == 1 -> HEADER_VIEW_TYPE
                else -> PULL_REQUEST_VIEW_TYPE
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SuggestedReviewersViewHolder -> holder.fillIn(reviewers)
            is HeaderViewHolder -> holder.fillIn(
                    if (pullRequests.isEmpty()) context.getString(R.string.no_pull_requests) else context.getString(
                            R.string.pull_requests_assigned_to_you, pullRequests.size))
            is PullRequestViewHolder -> {
                holder.fillIn(pullRequests[position - 2])
                holder.showDivider(position != 2)
            }
        }
    }

    private inner class SuggestedReviewersViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val suggestedReviewersViews: Array<Pair<ImageView, TextView>> by lazy {
            arrayOf(itemView.childImageView(R.id.firstReviewer) to itemView.childTextView(R.id.firstReviewerName),
                    itemView.childImageView(R.id.leadReviewer) to itemView.childTextView(R.id.leadReviewerName),
                    itemView.childImageView(R.id.secondReviewer) to itemView.childTextView(R.id.secondReviewerName))
        }

        fun fillIn(reviewersInformation: ReviewersInformation) {
            reviewersInformation.apply {
                val reviewersCount = preferredReviewers.size
                for (index in 0..Math.min(reviewersCount - 1, suggestedReviewersViews.size - 1)) {
                    suggestedReviewersViews[index].apply {
                        val reviewerUser = preferredReviewers[index].user
                        first.visibility = View.VISIBLE
                        second.visibility = View.VISIBLE
                        val animation = AnimationUtils.loadAnimation(context, R.anim.bounce).apply {
                            interpolator = BounceInterpolator(0.15, 20.0)
                            startOffset = index * 200L
                        }


                        avatarRequestsConsumer.accept(AvatarLoadRequest(reviewerUser, ImageViewTarget(first, animation)))
                        second.text = reviewerUser.displayName
                        second.alpha = 0f
                        second.animate().alpha(1f).setStartDelay(index * 200L).start()
                    }
                }

                for (index in reviewersCount..suggestedReviewersViews.size - 1) {
                    suggestedReviewersViews[index].apply {
                        first.visibility = View.GONE
                        second.visibility = View.GONE
                    }
                }
            }
        }
    }

    private inner class HeaderViewHolder constructor(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun fillIn(text: String) {
            (itemView as TextView).text = text
        }
    }
}