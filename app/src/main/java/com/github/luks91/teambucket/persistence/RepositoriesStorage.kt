/**
 * Copyright (c) 2017-present, Team Bucket Contributors.

 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.github.luks91.teambucket.persistence

import com.github.luks91.teambucket.ReactiveBus
import com.github.luks91.teambucket.model.Project
import com.github.luks91.teambucket.model.Repository
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.realm.RealmModel
import io.realm.RealmQuery

class RepositoriesStorage(private val eventsBus: ReactiveBus) {
    private val scheduler by RealmSchedulerHolder
    companion object {
        const val REPOSITORIES_REALM = "projects.realm"
    }

    fun selectedRepositories(sortColumn: String = "slug", notifyIfMissing: Boolean = true): Observable<List<Repository>> {
        return usingRealm(REPOSITORIES_REALM, scheduler) { realm ->
            realm.where(RealmRepository::class.java).findAllSorted(sortColumn).asFlowable()
                    .map { it.toList() }
                    .map { it.map { it.toRepository() } }
                    .doOnNext { list ->
                        if (list.isEmpty() && notifyIfMissing) {
                            eventsBus.post(ReactiveBus.EventRepositoriesMissing(PullRequestsStorage::class.java.simpleName))
                        }
                    }
                    .toObservable()
        }
    }

    fun selectedProjects(sortColumn: String = "key"): Observable<List<Project>> {
        return usingRealm(REPOSITORIES_REALM, scheduler) { realm ->
            realm.where(RealmProject::class.java).findAllSorted(sortColumn).asFlowable()
                    .map { it.toList() }.map { it.map { it.toProject() } }.toObservable()
        }
    }

    fun subscribeRepositoriesPersisting(projects: Observable<List<Project>>, repositories: Observable<List<Repository>>)
            : Disposable {

        return usingRealm(REPOSITORIES_REALM, scheduler) { realm ->
            Observable.zip<List<RealmProject>, List<RealmRepository>, Unit>(
                    projects
                            .map { projects -> projects.map { project -> RealmProject.from(project) } }
                            .observeOn(scheduler),
                    repositories
                            .map { repositories -> repositories.map { repository -> RealmRepository.Factory.from(repository) } }
                            .observeOn(scheduler),
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

    private inline fun <Stored: RealmModel, Compared: RealmModel> RealmQuery<Stored>.allNotExisting(
            comparedList: List<Compared>, storedColumn: String, getComparedColumn: (Compared) -> String): RealmQuery<Stored> {

        if (comparedList.isEmpty()) {
            return this
        }

        var query = this.beginGroup()
        comparedList.forEach { query = query.notEqualTo(storedColumn, getComparedColumn(it)) }
        return query.endGroup()
    }
}
