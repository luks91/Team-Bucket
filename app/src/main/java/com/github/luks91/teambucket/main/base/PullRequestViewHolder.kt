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

import android.support.annotation.DrawableRes
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.model.*
import com.github.luks91.teambucket.util.ImageViewTarget
import com.github.luks91.teambucket.util.childImageView
import com.github.luks91.teambucket.util.childTextView
import com.github.luks91.teambucket.util.toMMMddDateString
import io.reactivex.functions.Consumer

class PullRequestViewHolder internal constructor(itemView: View, private val avatarRequestsConsumer: Consumer<AvatarLoadRequest>)
    : RecyclerView.ViewHolder(itemView) {

    private val authorName: TextView by lazy { itemView.childTextView(R.id.reviewAuthor) }
    private val authorAvatar: ImageView by lazy { itemView.childImageView(R.id.authorAvatar) }
    private val pullRequestUpdateDate: TextView by lazy {itemView.childTextView(R.id.pullRequestUpdateDate) }
    private val dividerView: View by lazy { itemView.findViewById(R.id.card_divider) }

    private val reviewers: Array<Pair<ImageView, ImageView>> by lazy {
        arrayOf(itemView.childImageView(R.id.firstReviewer) to itemView.childImageView(R.id.firstReviewerState),
                itemView.childImageView(R.id.secondReviewer) to itemView.childImageView(R.id.secondReviewerState),
                itemView.childImageView(R.id.thirdReviewer) to itemView.childImageView(R.id.thirdReviewerState),
                itemView.childImageView(R.id.fourthReviewer) to itemView.childImageView(R.id.fourthReviewerState))
    }

    private val reviewTitle: TextView by lazy { itemView.findViewById(R.id.reviewTitle) as TextView }
    private val reviewBranch: TextView by lazy { itemView.findViewById(R.id.reviewBranch) as TextView }
    private val targetBranch: TextView by lazy { itemView.findViewById(R.id.targetBranch) as TextView }

    fun showDivider(display: Boolean) {
        dividerView.visibility = if (display) View.VISIBLE else View.INVISIBLE
    }

    fun fillIn(pullRequest: PullRequest) {
        authorName.text = pullRequest.author.user.displayName
        reviewTitle.text = pullRequest.title
        reviewBranch.text = pullRequest.sourceBranch.displayId
        targetBranch.text = pullRequest.targetBranch.displayId
        avatarRequestsConsumer.accept(AvatarLoadRequest(pullRequest.author.user, ImageViewTarget(authorAvatar)))

        val pullRequestReviewers = pullRequest.reviewers
        for ((index, reviewViews) in reviewers.withIndex()) {
            if (pullRequestReviewers.size > index) {
                val pullRequestMember = pullRequestReviewers[index]
                reviewViews.apply {
                    avatarRequestsConsumer.accept(AvatarLoadRequest(pullRequestMember.user, ImageViewTarget(first)))
                    first.visibility = View.VISIBLE
                    second.visibility = View.VISIBLE
                    second.setImageResource(resourceFromReviewerState(pullRequestMember))
                }
            } else {
                reviewViews.apply {
                    first.visibility = View.GONE
                    second.visibility = View.GONE
                }
            }
        }

        pullRequestUpdateDate.text = pullRequest.updatedDate.toMMMddDateString()
        pullRequestUpdateDate.setTextColor(
                if (pullRequest.isLazilyReviewed()) itemView.context.getColor(R.color.warning_red_text)
                    else itemView.context.getColor(R.color.secondary_text))
    }

    private @DrawableRes fun resourceFromReviewerState(member: PullRequestMember): Int {
        when (member.status) {
            APPROVED -> return R.drawable.ic_approved_24dp
            NEEDS_WORK -> return R.drawable.ic_needs_work_24dp
            else -> return 0
        }
    }
}