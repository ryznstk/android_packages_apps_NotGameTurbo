/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

class AppSettingsActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra(EXTRA_PACKAGE)?.let { pkg ->
            runCatching {
                title =
                    packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0))
            }
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, AppSettingsFragment())
            .commit()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
    }
}
