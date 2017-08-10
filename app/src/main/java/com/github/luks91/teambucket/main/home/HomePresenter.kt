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
import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.TeamMembersProvider
import com.github.luks91.teambucket.connection.ConnectionProvider
import com.github.luks91.teambucket.di.AppContext
import com.github.luks91.teambucket.main.base.BasePullRequestsPresenter
import com.github.luks91.teambucket.persistence.PersistenceProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class HomePresenter @Inject constructor(@AppContext context: Context, private val connectionProvider: ConnectionProvider,
                                        private val persistenceProvider: PersistenceProvider,
                                        teamMembersProvider: TeamMembersProvider, eventsBus: ReactiveBus):
        BasePullRequestsPresenter<HomeView>(context, connectionProvider, persistenceProvider, teamMembersProvider, eventsBus) {

    private var disposable = Disposables.empty()

    override fun attachView(view: HomeView) {
        super.attachView(view)
        disposable = connectionProvider.obtainConnection()
                .switchMap { persistenceProvider.pullRequestsUnderReviewBy(it.userName) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.onUserPullRequestsProvided(it) }
    }

    override fun avatarSize(): Int = 256

    override fun detachView(retainInstance: Boolean) {
        super.detachView(retainInstance)
        disposable.dispose()
    }

}
