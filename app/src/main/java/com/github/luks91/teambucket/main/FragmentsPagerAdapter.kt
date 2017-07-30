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

import android.content.res.Resources
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.github.luks91.teambucket.R
import com.github.luks91.teambucket.main.home.HomeFragment
import com.github.luks91.teambucket.main.reviewers.ReviewersFragment
import com.github.luks91.teambucket.main.statistics.StatisticsFragment

class FragmentsPagerAdapter(fragmentManager: FragmentManager, val resources: Resources)
    : FragmentStatePagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        return ViewPagerPage.values()[position].getItem()
    }

    override fun getCount(): Int {
        return ViewPagerPage.values().size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return ViewPagerPage.values()[position].getTitle(resources)
    }

    enum class ViewPagerPage {
        HOME {
            override fun getItem(): Fragment {
                return HomeFragment.newInstance()
            }

            override fun getTitle(res: Resources): CharSequence {
                return res.getString(R.string.home_tab_title)
            }
        },
        REVIEWERS {
            override fun getItem(): Fragment {
                return ReviewersFragment.newInstance()
            }

            override fun getTitle(res: Resources): CharSequence {
                return res.getString(R.string.reviewers_tab_title)
            }
        },
        STATISTICS {
            override fun getItem(): Fragment {
                return StatisticsFragment.newInstance()
            }

            override fun getTitle(res: Resources): CharSequence {
                return res.getString(R.string.statistics_tab_title)
            }
        };

        abstract fun getItem(): Fragment
        abstract fun getTitle(res: Resources): CharSequence
    }
}