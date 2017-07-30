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

package com.github.luks91.teambucket.main

import android.support.annotation.StringRes
import com.github.luks91.teambucket.connection.ConnectionProvider
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.model.*
import com.github.luks91.teambucket.persistence.PersistenceProvider
import com.github.luks91.teambucket.connection.BitbucketApi
import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.util.MatchedResources
import com.github.luks91.teambucket.util.matchSortedWith
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainPresenter @Inject constructor(val connectionProvider: ConnectionProvider,
                                        val persistenceProvider: PersistenceProvider,
                                        val eventsBus: ReactiveBus) : MvpPresenter<MainView> {

    private var disposable = Disposables.empty()

    override fun attachView(view: MainView) {
        val requestUserCredentialsObservable = view.requestUserCredentials().publish().refCount()
        val connection = connectionProvider.obtainConnection().publish()

        val projectsSelection = Observable.combineLatest(
                        connection, eventsBus.receive(ReactiveBus.EventRepositoriesMissing::class.java)
                                .debounce(100, TimeUnit.MILLISECONDS)
                                .cast(Any::class.java)
                                .mergeWith(view.intentRepositorySettings()),
                        BiFunction<BitbucketConnection, Any, Observable<List<Project>>> {
                            (_, _, api, token), _ -> projectSelection(api, token, view)
                        }
                ).switchMap { obs -> obs }.publish()

        val repositoriesSelection =
                Observable.combineLatest(
                        connection, projectsSelection,
                        BiFunction<BitbucketConnection, List<Project>, Observable<List<Repository>>> {
                            (_, _, api, token), projects -> repositoriesSelection(projects, api, token, view)
                        }
                ).switchMap { obs -> obs }.publish()

        disposable = CompositeDisposable(
                eventsBus.receive(ReactiveBus.EventCredentialsInvalid::class.java)
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap { requestUserCredentialsObservable }
                        .subscribe { credentials -> eventsBus.post(credentials) },
                persistenceProvider.subscribeRepositoriesPersisting(projectsSelection, repositoriesSelection),
                eventsBus.receive(ReactiveBus.EventNoNetworkConnection::class.java)
                        .debounce(100, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { _ -> view.showNoNetworkNotification() },
                repositoriesSelection.connect(),
                projectsSelection.connect(),
                connection.connect()
        )
    }

    private fun projectSelection(api: BitbucketApi, token: String, view: MainView): Observable<List<Project>> {
        return BitbucketApi.queryPaged { start -> api.getProjects(token, start) }
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(BitbucketApi.handleNetworkError(eventsBus, MainPresenter::class.java.simpleName))
                .reduce { t1, t2 -> t1 + t2 }.toObservable()
                .withLatestFrom(
                        persistenceProvider.selectedProjects(sortColumn = "key"),
                        BiFunction<List<Project>, List<Project>, Observable<List<Project>>> {
                            remoteProjects, localProjects ->
                            requestUserToSelectFrom(
                                    remoteProjects.sortedBy { it.key }
                                            .matchSortedWith(localProjects, compareBy<Project> { it.key }),
                                    view, R.string.projects_choose_header, { data -> data.name })
                        }
                ).concatMap { obs -> obs }
    }

    private inline fun <T> requestUserToSelectFrom(resources: MatchedResources<T>, view: MainView, @StringRes headerText: Int,
                                                   crossinline labelFunction: (T) -> String): Observable<List<T>> {
        return Observable.just(resources.resources).zipWith(
                Observable.just(resources)
                        .observeOn(AndroidSchedulers.mainThread())
                        .concatMap { resources -> view.requestToSelectFrom(headerText, resources.resources.map(labelFunction),
                        resources.matchedIndices.toIntArray())},
                BiFunction<List<T>, List<Int>, List<T>> {
                    projects, selection ->
                    projects.filterIndexed { index, _ -> selection.contains(index) }
                })
    }

    private fun repositoriesSelection(projects: List<Project>, api: BitbucketApi, token: String, view: MainView)
            : Observable<List<Repository>> {

        return Observable.fromIterable(projects).flatMap { (key) -> Observable.empty<List<Repository>>()
                        BitbucketApi.queryPaged { start -> api.getProjectRepositories(token, key, start) }
                                .subscribeOn(Schedulers.io())
                    }
                .onErrorResumeNext(BitbucketApi.handleNetworkError(eventsBus, MainPresenter::class.java.simpleName))
                .reduce { t1, t2 -> t1 + t2 }.toObservable()
                .withLatestFrom(
                        persistenceProvider.selectedRepositories(sortColumn = "slug", notifyIfMissing = false),
                        BiFunction<List<Repository>, List<Repository>, Observable<List<Repository>>> {
                            remoteRepositories, remoteProjects ->
                            requestUserToSelectFrom(
                                    remoteRepositories.sortedBy { it.slug }
                                            .matchSortedWith(remoteProjects, compareBy<Repository> { it.slug }),
                                    view, R.string.repositories_choose_header, { data -> data.name })
                        }
                ).concatMap { obs -> obs }
                .first(listOf<Repository>())
                .toObservable()
    }

    override fun detachView(retainInstance: Boolean) {
        disposable.dispose()
    }
}