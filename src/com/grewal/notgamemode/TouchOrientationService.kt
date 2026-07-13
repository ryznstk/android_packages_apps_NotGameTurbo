/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.util.Log
import android.view.Display

class TouchOrientationService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateOrientation()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateOrientation() {
        val rotation =
            getSystemService(DisplayManager::class.java)
                .getDisplay(Display.DEFAULT_DISPLAY)
                ?.rotation ?: return
        Log.i(TAG, "orientation update, rotation: $rotation")
        TouchFeatureManager.setPanelOrientation(rotation)
    }

    companion object {
        private const val TAG = "TouchOrientationService"
    }
}
