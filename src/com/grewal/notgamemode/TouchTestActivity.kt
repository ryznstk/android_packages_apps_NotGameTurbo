/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Switch

class TouchTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this)
        root.addView(TouchTestView(this), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val margin = (24 * resources.displayMetrics.density).toInt()
        val toggle =
            Switch(this).apply {
                text = getString(R.string.touch_test_game_mode)
                setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, checked ->
                    TouchFeatureManager.setGameMode(checked)
                }
            }
        root.addView(
            toggle,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
                topMargin = margin
                marginEnd = margin
            },
        )

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()

        GameModeService.manualOverride = true
    }

    override fun onPause() {
        super.onPause()

        GameModeService.manualOverride = false
        TouchFeatureManager.setGameMode(false)
    }
}
