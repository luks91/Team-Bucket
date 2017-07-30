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

package com.github.luks91.teambucket.util

import java.util.Collections

fun <T> List<T>.matchSortedWith(sortedMatchList: List<T>, elementComparator: Comparator<T>): MatchedResources<T> {
    val matchedIndices = mutableListOf<Int>()
    var matchListIndex = 0
    for ((index, resource) in this.withIndex()) {
        while (matchListIndex < sortedMatchList.size
                && elementComparator.compare(sortedMatchList[matchListIndex], resource) < 0) {
            matchListIndex++
        }

        if (matchListIndex < sortedMatchList.size && elementComparator.compare(sortedMatchList[matchListIndex], resource) == 0) {
            matchedIndices.add(index)
        }
    }
    return MatchedResources(this, Collections.unmodifiableList(matchedIndices))
}

data class MatchedResources<out T>(val resources: List<T>, val matchedIndices: List<Int>)