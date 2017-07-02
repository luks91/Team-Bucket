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

package com.github.luks91.teambucket

import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.github.luks91.teambucket.adapter.FragmentsPagerAdapter
import com.github.luks91.teambucket.presenter.MainPresenter
import com.hannesdorfmann.mosby3.mvp.MvpActivity
import io.reactivex.Observable
import com.afollestad.materialdialogs.MaterialDialog
import com.github.luks91.teambucket.model.*


class MainActivity : MainView, MvpActivity<MainView, MainPresenter>() {

    override fun createPresenter(): MainPresenter {
        return MainPresenter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById(R.id.viewpager) as ViewPager
        viewPager.adapter = FragmentsPagerAdapter(supportFragmentManager, resources)

        val tabLayout = findViewById(R.id.tabs) as TabLayout
        tabLayout.setupWithViewPager(viewPager)
    }

    override fun requestUserCredentials(): Observable<BitbucketCredentials> {
        return Observable.create<BitbucketCredentials>({
            emitter ->
                val dialog = MaterialDialog.Builder(this)
                    .title(R.string.bitbucket_auth_header)
                    .customView(R.layout.content_login_dialog, false)
                    .positiveText(R.string.confirm)
                    .cancelListener { emitter.onComplete() }
                    .onPositive { dialog, _ ->
                        val customView = dialog.customView!!
                        val usernameField = customView.findViewById(R.id.username) as EditText
                        val passwordField = customView.findViewById(R.id.password) as EditText
                        val serverUrl = customView.findViewById(R.id.bitbucketUrl) as EditText
                        emitter.onNext(BitbucketCredentials(
                                serverUrl.text.toString(),
                                usernameField.text.toString(),
                                passwordField.text.toString()))
                        emitter.onComplete()
                    }
                    .show()

            emitter.setCancellable {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        })
    }

    override fun requestToSelectFrom(@StringRes titleRes: Int, projects: List<String>): Observable<List<Int>> {
        return Observable.create<List<Int>>({
            emitter ->
                val dialog = MaterialDialog.Builder(this)
                        .title(titleRes)
                        .items(projects)
                        .itemsCallbackMultiChoice(null, { _, _, _ -> true })
                        .onPositive { dialog, _ ->
                            emitter.onNext(dialog.selectedIndices!!.asList())
                            emitter.onComplete()
                        }
                        .cancelListener {
                            emitter.onNext(listOf())
                            emitter.onComplete()
                        }
                        .positiveText(R.string.confirm).show()
            emitter.setCancellable {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        })
    }

    override fun showNoNetworkNotification() {
        Toast.makeText(this@MainActivity, R.string.toast_no_network, Toast.LENGTH_SHORT).show()
    }
}
