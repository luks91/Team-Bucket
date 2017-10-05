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

import android.content.Context
import com.github.luks91.teambucket.di.AppContext
import com.github.luks91.teambucket.model.Density
import com.github.luks91.teambucket.model.User
import io.reactivex.Observable
import io.reactivex.schedulers.Timed
import io.realm.Realm

class TeamMembersStorage(@AppContext context: Context) {
    private val scheduler by RealmSchedulerHolder

    init {
        Realm.init(context)
    }

    companion object {
        const val TEAM_MEMBERS_REALM = "team_members_realm"
    }

    fun persistTeamMembers(teamMembers: Timed<Map<User, Density>>) {
        usingRealm(TEAM_MEMBERS_REALM, scheduler) { realm ->
            Observable.just(teamMembers)
                    .observeOn(scheduler)
                    .map { RealmTeam.from(it) }
                    .map { teamMembers ->
                        realm.executeTransaction {
                            realm.delete(RealmDensifiedUser::class.java)
                            realm.delete(RealmUser::class.java)
                            realm.delete(RealmTeam::class.java)
                            realm.copyToRealmOrUpdate(teamMembers)
                        }
                    }
        }.subscribe()
    }

    fun teamMembers(): Observable<Timed<Map<User, Density>>> {
        return usingRealm(TEAM_MEMBERS_REALM, scheduler) { realm ->
            realm.where(RealmTeam::class.java).findAll().asFlowable()
                    .map { results -> results.first(RealmTeam(0L)) }
                    .map { team -> team.toTimedMap() }
                    .toObservable()
        }
    }
}
