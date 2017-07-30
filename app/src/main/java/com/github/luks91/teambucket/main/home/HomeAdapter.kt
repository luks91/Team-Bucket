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
import com.github.luks91.teambucket.model.ReviewersInformation
import com.github.luks91.teambucket.model.User
import com.github.luks91.teambucket.util.BounceInterpolator
import com.github.luks91.teambucket.util.ImageViewTarget
import com.github.luks91.teambucket.util.childImageView
import com.github.luks91.teambucket.util.childTextView
import com.squareup.picasso.Target

class HomeAdapter(private val context: Context, private val callback: HomeAdapter.Callback):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val EMPTY_VIEW_TYPE = 0
    val REVIEWERS_SUGGESTION_VIEW_TYPE = 1

    private var reviewers: ReviewersInformation = ReviewersInformation.EMPTY

    fun onReviewersReceived(reviewers: ReviewersInformation) {
        this.reviewers = reviewers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent!!.context)
        when (viewType) {
            EMPTY_VIEW_TYPE -> return object: RecyclerView.ViewHolder(inflater.inflate(R.layout.no_data_view, parent, false)) {}
            else -> return SuggestedReviewersViewHolder(inflater.inflate(R.layout.suggested_reviewers_card, parent, false))
        }
    }

    override fun getItemCount() = if (reviewers != ReviewersInformation.EMPTY) 1 else 0

    override fun getItemViewType(position: Int): Int =
            if (reviewers.reviewers.isEmpty()) EMPTY_VIEW_TYPE else REVIEWERS_SUGGESTION_VIEW_TYPE

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SuggestedReviewersViewHolder -> holder.fillIn(reviewers)
        }
    }

    inner class SuggestedReviewersViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val suggestedReviewersViews: Array<Pair<ImageView, TextView>> by lazy {
            arrayOf(itemView.childImageView(R.id.firstReviewer) to itemView.childTextView(R.id.firstReviewerName),
                    itemView.childImageView(R.id.leadReviewer) to itemView.childTextView(R.id.leadReviewerName),
                    itemView.childImageView(R.id.secondReviewer) to itemView.childTextView(R.id.secondReviewerName))
        }

        fun fillIn(reviewersInformation: ReviewersInformation) {
            reviewersInformation.apply {
                val reviewersCount = reviewers.size
                for (index in 0..Math.min(reviewersCount - 1, suggestedReviewersViews.size - 1)) {
                    suggestedReviewersViews[index].apply {
                        val reviewerUser = reviewers[index].user
                        first.visibility = View.VISIBLE
                        second.visibility = View.VISIBLE
                        val animation = AnimationUtils.loadAnimation(context, R.anim.bounce).apply {
                            interpolator = BounceInterpolator(0.15, 20.0)
                            startOffset = index * 200L
                        }
                        callback.loadImageFor(serverUrl, reviewerUser.avatarUrlSuffix, ImageViewTarget(first, animation))
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

    interface Callback {
        fun showPullRequestsFor(user: User)
        fun loadImageFor(serverUrl: String, urlPath: String, target: Target)
    }
}