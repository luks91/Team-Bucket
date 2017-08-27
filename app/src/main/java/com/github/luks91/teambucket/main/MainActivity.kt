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

import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.hannesdorfmann.mosby3.mvp.MvpActivity
import io.reactivex.Observable
import com.afollestad.materialdialogs.MaterialDialog
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.model.BitbucketCredentials
import com.github.luks91.teambucket.util.stringText
import dagger.android.AndroidInjection
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject
import dagger.android.DispatchingAndroidInjector
import dagger.android.AndroidInjector
import io.reactivex.subjects.PublishSubject
import android.content.Intent
import android.net.Uri


class MainActivity : MainView, MvpActivity<MainView, MainPresenter>(), HasSupportFragmentInjector {

    private val updateRepositoriesIntents = PublishSubject.create<Any>()

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var mainPresenter: MainPresenter

    @Inject
    lateinit var fragmentsPagerAdapter: FragmentsPagerAdapter

    override fun createPresenter(): MainPresenter = mainPresenter

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentDispatchingAndroidInjector

    override fun intentRepositorySettings(): Observable<Any> = updateRepositoriesIntents

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (findViewById(R.id.viewpager) as ViewPager).apply {
            adapter = fragmentsPagerAdapter
            val tabLayout = this@MainActivity.findViewById(R.id.tabs) as TabLayout
            tabLayout.setupWithViewPager(this)
        }

        findViewById(R.id.mainActivityFab).setOnClickListener { updateRepositoriesIntents.onNext(Object()) }
        (findViewById(R.id.main_toolbar) as Toolbar).apply{
            setSupportActionBar(this)
            supportActionBar!!.setDisplayShowTitleEnabled(true)
            supportActionBar!!.setTitle(R.string.app_name)
        }
    }

    override fun requestUserCredentials(previousCredentials: BitbucketCredentials): Observable<BitbucketCredentials> {
        return Observable.create<BitbucketCredentials>({
            emitter ->
                val dialog = MaterialDialog.Builder(this)
                    .title(R.string.bitbucket_auth_header)
                    .customView(R.layout.content_login_dialog, false)
                    .positiveText(R.string.confirm)
                    .cancelListener {
                        emitter.onNext(previousCredentials)
                        emitter.onComplete()
                    }
                    .onPositive { dialog, _ ->
                        dialog.customView!!.apply {
                            val usernameField = this@apply.findViewById(R.id.username) as EditText
                            val passwordField = this@apply.findViewById(R.id.password) as EditText
                            val serverUrl = this@apply.findViewById(R.id.bitbucketUrl) as EditText
                            emitter.apply {
                                onNext(BitbucketCredentials(serverUrl.stringText(),
                                        usernameField.stringText(),
                                        passwordField.stringText()))
                                onComplete()
                            }
                        }
                    }
                    .show()

            dialog.customView!!.apply {
                (this@apply.findViewById(R.id.username) as EditText).setText(previousCredentials.username)
                (this@apply.findViewById(R.id.bitbucketUrl) as EditText).setText(previousCredentials.bitBucketUrl)
            }

            emitter.setCancellable {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        })
    }

    override fun requestToSelectFrom(@StringRes titleRes: Int, resources: List<String>, selectedIndices: IntArray)
            : Observable<List<Int>> {
        return Observable.create<List<Int>>({
            emitter ->
                val dialog = MaterialDialog.Builder(this)
                        .title(titleRes)
                        .items(resources)
                        .itemsCallbackMultiChoice(selectedIndices.toTypedArray(), { _, _, _ -> true })
                        .onPositive { dialog, _ ->
                            emitter.onNext(dialog.selectedIndices!!.asList())
                            emitter.onComplete()
                        }
                        .cancelListener {
                            emitter.onNext(selectedIndices.toList())
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

    override fun showErrorNotification(message: Int) = Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_info -> {
                MaterialDialog.Builder(this)
                        .title(R.string.about)
                        .items(R.array.copyright)
                        .onPositive { dialog, _ -> dialog.dismiss() }
                        .autoDismiss(false)
                        .itemsCallback { _, _, position, _ ->
                            val url = resources.getStringArray(R.array.copyright_urls)[position]
                            if (!url.isNullOrEmpty()) {
                                startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
                            }
                        }
                        .itemsLongCallback { _, _, position, _ ->
                            val magicUrls = resources.getStringArray(R.array.magic_urls)
                            if (position < magicUrls.size) {
                                startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(magicUrls[position])))
                            }
                            return@itemsLongCallback true
                        }
                        .positiveText(R.string.confirm).show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun displayUserName(name: String) {
        supportActionBar!!.title = name
    }

}