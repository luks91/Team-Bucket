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

package com.github.luks91.prparadise.presenter

import android.content.Context
import com.github.luks91.prparadise.MainView
import com.github.luks91.prparadise.R
import com.github.luks91.prparadise.model.*
import com.github.luks91.prparadise.persistence.PersistenceProvider
import com.github.luks91.prparadise.rest.BitbucketApi
import com.github.luks91.prparadise.util.ReactiveBus
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class MainPresenter(context: Context) : MvpPresenter<MainView> {

    private val connectionProvider: ConnectionProvider =
            ConnectionProvider(context, context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE))
    private val persistenceProvider: PersistenceProvider = PersistenceProvider(context)

    private var credentialsProvidingSubscription: Disposable = Disposables.empty()
    private var repositoriesProvidingSubscription: Disposable = Disposables.empty()
    private var noNetworkSubscription: Disposable = Disposables.empty()
    private var dataPersistenceSubscription: Disposable = Disposables.empty()

    override fun attachView(view: MainView) {
        val requestUserCredentialsObservable = view.requestUserCredentials().publish().refCount()
        credentialsProvidingSubscription = ReactiveBus.INSTANCE.receive(ReactiveBus.EventCredentialsInvalid::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .concatMap { requestUserCredentialsObservable }
                .subscribe { credentials -> ReactiveBus.INSTANCE.post(credentials) }

        dataPersistenceSubscription = persistenceProvider.subscribeRepositoriesPersisting()

        val requestRepositoriesSelection = connectionProvider.obtainConnection().flatMap { connection ->
                    repositoriesSelection(connection, view, projectSelection(connection, view))
                }.publish().refCount()

        repositoriesProvidingSubscription = ReactiveBus.INSTANCE.receive(ReactiveBus.EventRepositoriesMissing::class.java)
                .switchMap { requestRepositoriesSelection.firstOrError().toObservable() }
                .subscribe({ repositories -> ReactiveBus.INSTANCE.post(ReactiveBus.EventRepositories(
                        MainPresenter::class.java.simpleName, repositories)) })

        noNetworkSubscription = ReactiveBus.INSTANCE.receive(ReactiveBus.EventNoNetworkConnection::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ -> view.showNoNetworkNotification() }
    }

    fun projectSelection(connection: BitbucketConnection, view: MainView): Observable<Project> {
        return Observable.just(connection).flatMap { (_, api, token) ->
                        BitbucketApi.queryPaged { start -> api.getProjects(token, start) }
                                .subscribeOn(Schedulers.io())
                    }
                    .onErrorResumeNext(BitbucketApi.handleNetworkError(MainPresenter::class.java.simpleName))
                    .reduce { t1, t2 -> t1 + t2 }.toObservable()
                    .concatMap { projects -> requestUserToSelect(projects, view, { data -> data.name }) }
                    .concatMapIterable { list -> list }
    }

    fun <T> requestUserToSelect(from: List<T>, view: MainView, labelFunction: (T) -> String): Observable<List<T>> {
        return Observable.just(from).zipWith(
                Observable.just(from)
                        .map { list -> list.map { item -> labelFunction.invoke(item) } }
                        .observeOn(AndroidSchedulers.mainThread())
                        .concatMap { list -> view.requestToSelectFrom(R.string.projects_choose_header, list) },
                BiFunction<List<T>, List<Int>, List<T>> {
                    projects, selection ->
                    projects.filterIndexed { index, _ -> selection.contains(index) }
                })
    }

    fun repositoriesSelection(connection: BitbucketConnection, view: MainView, projects: Observable<Project>)
            : Observable<List<Repository>> {

        return Observable.just(connection)
                .flatMap { (_, api, token) ->
                    projects.flatMap { (key) -> Observable.empty<List<Repository>>()
                        BitbucketApi.queryPaged { start -> api.getProjectRepositories(token, key, start) }
                                .subscribeOn(Schedulers.io())
                    }
                    .onErrorResumeNext(BitbucketApi.handleNetworkError(MainPresenter::class.java.simpleName))
                }
                .reduce { t1, t2 -> t1 + t2 }.toObservable()
                .map { repositories -> repositories.sortedBy { repository -> repository.name.toLowerCase() } }
                .concatMap { repositories -> requestUserToSelect(repositories, view, { data -> data.name }) }
                .first(listOf<Repository>())
                .toObservable()
    }

    override fun detachView(retainInstance: Boolean) {
        credentialsProvidingSubscription.dispose()
        dataPersistenceSubscription.dispose()
        repositoriesProvidingSubscription.dispose()
        noNetworkSubscription.dispose()
    }
}