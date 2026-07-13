/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.content.Intent
import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

class GameModeActivity : CollapsingToolbarBaseActivity() {

    companion object {
        private const val TAG = "GameModeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, GameModeFragment(), TAG)
            .commit()

        startService(Intent(this, GameModeService::class.java))
    }
}
