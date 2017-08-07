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

package com.github.luks91.teambucket.persistence

import android.content.Context
import android.os.HandlerThread
import com.github.luks91.teambucket.di.AppContext
import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.model.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Timed
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmModel
import io.realm.RealmQuery
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PersistenceProvider @Inject constructor(@AppContext context: Context, private val eventsBus: ReactiveBus) {

    init {
        Realm.init(context)
    }

    companion object Holder {
        val looperScheduler: Scheduler
        init {
            val thread = HandlerThread("Realm worker thread")
            thread.start()
            looperScheduler = AndroidSchedulers.from(thread.looper)
        }
    }

    fun selectedRepositories(sortColumn: String = "slug", notifyIfMissing: Boolean = true): Observable<List<Repository>> {
        return usingRealm { realm ->
            realm.where(RealmRepository::class.java).findAllSorted(sortColumn).asFlowable()
                    .map { it.toList() }
                    .map { it.map { it.toRepository() } }
                    .doOnNext { list ->
                        if (list.isEmpty() && notifyIfMissing) {
                            eventsBus.post(ReactiveBus.EventRepositoriesMissing(PersistenceProvider::class.java.simpleName))
                        }
                    }
                    .toObservable()
        }
    }

    fun selectedProjects(sortColumn: String = "key"): Observable<List<Project>> {
        return usingRealm { realm ->
            realm.where(RealmProject::class.java).findAllSorted(sortColumn).asFlowable()
                    .map { it.toList() }.map { it.map { it.toProject() } }.toObservable()
        }
    }

    private fun <T> usingRealm(function: (realm: Realm) -> Observable<T>): Observable<T> {
        return io.reactivex.Observable.using(
                { Realm.getInstance(RealmConfiguration.Builder().name("projects_realm").deleteRealmIfMigrationNeeded().build()) },
                { realm -> function.invoke(realm) },
                { realm -> realm.close() } )
                .subscribeOn(looperScheduler)
                .unsubscribeOn(looperScheduler)
    }

    private inline fun <Stored: RealmModel, Compared: RealmModel> RealmQuery<Stored>.allNotExisting(
            comparedList: List<Compared>, storedColumn: String, getComparedColumn: (Compared) -> String): RealmQuery<Stored> {

        if (comparedList.isEmpty()) {
            return this
        }

        var query = this.beginGroup()
        comparedList.forEach { query = query.notEqualTo(storedColumn, getComparedColumn(it)) }
        return query.endGroup()
    }

    fun subscribeRepositoriesPersisting(projects: Observable<List<Project>>, repositories: Observable<List<Repository>>)
            : Disposable {

        return usingRealm { realm ->
            Observable.zip<List<RealmProject>, List<RealmRepository>, Unit>(
                    projects
                            .map { projects -> projects.map { project -> RealmProject.from(project) } }
                            .observeOn(looperScheduler),
                    repositories
                            .map { repositories -> repositories.map { repository -> RealmRepository.Factory.from(repository) } }
                            .observeOn(looperScheduler),
                    BiFunction<List<RealmProject>, List<RealmRepository>, Unit> { projects, repositories ->
                        realm.executeTransaction {
                            realm.where(RealmProject::class.java)
                                    .allNotExisting(projects, "key", { it.key })
                                    .findAll().deleteAllFromRealm()
                            realm.copyToRealmOrUpdate(projects)

                            realm.where(RealmRepository::class.java)
                                    .allNotExisting(projects, "project.key", { it.key })
                                    .findAll().deleteAllFromRealm()

                            realm.where(RealmRepository::class.java)
                                    .allNotExisting(repositories, "slug", { it.slug })
                                    .findAll().deleteAllFromRealm()
                            realm.copyToRealmOrUpdate(repositories)
                        }
                    })
        }.subscribe()
    }

    fun pullRequestsPersisting(pullRequests: Observable<List<PullRequest>>): Disposable {
        return usingRealm { realm ->
            pullRequests
                    .observeOn(looperScheduler)
                    .map { pullRequest -> pullRequest.map(RealmPullRequest.Factory::from) }
                    .map { pullRequests ->
                        realm.executeTransaction {
                            //TODO: removing all the rows and writing them again is not very efficient (though we're not dealing
                            //TODO: with much data as of right now). Consider improving performance in this area.
                            realm.delete(RealmPullRequest::class.java)
                            realm.delete(RealmPullRequestMember::class.java)
                            realm.delete(RealmUser::class.java)
                            realm.delete(RealmGitReference::class.java)
                            realm.copyToRealmOrUpdate(pullRequests)
                        }
                    }
        }.subscribe()
    }

    fun pullRequestsUnderReviewBy(userSlug: String): Observable<List<PullRequest>> {
        return usingRealm { realm ->
            realm.where(RealmPullRequestMember::class.java)
                    .equalTo("user.slug", userSlug)
                    .equalTo("approved", false)
                    .equalTo("role", "REVIEWER")
                    .findAll().asFlowable()
                    .concatMap { reviewers -> Flowable.fromIterable(reviewers)
                            .concatMapIterable{ member -> member.reviewingPullRequests }
                            .map { realmPullRequest -> realmPullRequest.toPullRequest() }
                            .toList().toFlowable()
                    }.toObservable()
        }.subscribeOn(looperScheduler)
    }

    fun teamMembersPersisting(teamMembers: Observable<Timed<Map<User, Density>>>): Observable<Any> {
        //TODO-#11: Store team members in Realm
        return teamMembers.cast(Any::class.java)
    }

    fun teamMembers(): Observable<Timed<Map<User, Density>>> {
        //TODO-#11: Store team members in Realm
        return Observable.just(Timed<Map<User, Density>>(mapOf(), System.currentTimeMillis(), TimeUnit.MILLISECONDS))
    }
}