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

package com.github.luks91.prparadise.persistence

import com.github.luks91.prparadise.model.EMPTY_STRING
import com.github.luks91.prparadise.model.Project
import com.github.luks91.prparadise.model.Repository
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

//We don't want to expose mutable model outside of this usage. All the modules and streams have to work with mutable
//model classes specified in .model package. As of Q2 2017, realm does not work with Kotlin data classes.
@RealmClass
internal open class RealmProject(@PrimaryKey open var key: String = EMPTY_STRING,
                                 open var name: String = EMPTY_STRING,
                                 open var description: String = EMPTY_STRING): RealmModel {
    fun toProject(): Project {
        return Project(key, name, description)
    }
}

@RealmClass
internal open class RealmRepository(
        @PrimaryKey open var slug: String = EMPTY_STRING,
        open var name: String = EMPTY_STRING,
        open var project: RealmProject = RealmProject()): RealmModel {

    fun toRepository(): Repository {
        return Repository(slug, name, project.toProject())
    }

    companion object Factory {
        fun from(repository: Repository): RealmRepository {
            val modelProject = repository.project
            val realmProject = RealmProject(modelProject.key, modelProject.name, modelProject.description)
            return RealmRepository(repository.slug, repository.name, realmProject)
        }
    }
}