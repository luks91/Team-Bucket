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

package com.github.luks91.teambucket

import com.github.luks91.teambucket.model.ImageLoadRequest
import com.github.luks91.teambucket.model.PullRequest
import com.github.luks91.teambucket.model.ReviewersInformation
import com.github.luks91.teambucket.model.User
import com.hannesdorfmann.mosby3.mvp.MvpView
import io.reactivex.Observable

interface ReviewersView : MvpView {

    fun onReviewersReceived(reviewers: ReviewersInformation)
    fun onLoadingCompleted()
    fun onPullRequestsProvided(reviews: List<IndexedValue<PullRequest>>)
    fun intentPullToRefresh(): Observable<Any>
    fun intentRetrieveReviews(): Observable<IndexedValue<User>>
    fun intentLoadAvatarImage(): Observable<ImageLoadRequest>
}