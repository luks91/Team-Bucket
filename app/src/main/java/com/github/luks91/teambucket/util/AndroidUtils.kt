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

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.support.annotation.IdRes
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView

fun EditText.stringText(): String {
    return this.text.toString()
}

@SuppressLint("ApplySharedPref")
fun SharedPreferences.edit(func: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.apply(func)
    editor.commit()
}

fun View.childTextView(@IdRes viewId: Int) = findViewById(viewId) as TextView
fun View.childImageView(@IdRes viewId: Int) = findViewById(viewId) as ImageView

fun SharedPreferences.Editor.put(pair: Pair<String, Any>) {
    val key = pair.first
    val value = pair.second
    when(value) {
        is String -> putString(key, value)
        is Int -> putInt(key, value)
        is Boolean -> putBoolean(key, value)
        is Long -> putLong(key, value)
        is Float -> putFloat(key, value)
        else -> error("Only primitive types can be stored in SharedPreferences")
    }
}