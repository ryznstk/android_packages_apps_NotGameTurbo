/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.os.Binder
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import vendor.xiaomi.hw.touchfeature.ITouchFeature

object TouchFeatureManager {

    private const val TAG = "GameModeTouchFeature"

    private const val TOUCH_ID = 0
    private const val TOUCH_GAME_MODE = 0
    private const val TOUCH_SUPER_REPORT = 202
    private const val TOUCH_PANEL_ORIENTATION = 8

    @Volatile private var touchFeature: ITouchFeature? = null

    private val deathRecipient =
        IBinder.DeathRecipient {
            Log.w(TAG, "touchfeature service died")
            touchFeature = null
        }

    @Synchronized
    private fun getService(): ITouchFeature? =
        touchFeature
            ?: runCatching {
                    val fqName = "${ITouchFeature.DESCRIPTOR}/default"
                    val binder = Binder.allowBlocking(ServiceManager.waitForDeclaredService(fqName))
                    ITouchFeature.Stub.asInterface(binder).apply {
                        asBinder().linkToDeath(deathRecipient, 0)
                    }
                }
                .onSuccess { touchFeature = it }
                .onFailure { e -> Log.e(TAG, "failed to get touchfeature service", e) }
                .getOrNull()

    private fun setModeValue(mode: Int, value: Int) {
        val service =
            getService()
                ?: run {
                    Log.e(TAG, "touchfeature service is null, cannot set mode $mode")
                    return
                }
        runCatching { service.setModeValue(TOUCH_ID, mode, value) }
            .onFailure { e -> Log.e(TAG, "setModeValue(mode=$mode, value=$value) failed", e) }
    }

    fun setGameMode(enabled: Boolean) {
        Log.i(TAG, "setGameMode: $enabled")
        val value = if (enabled) 1 else 0
        setModeValue(TOUCH_GAME_MODE, value)
        setModeValue(TOUCH_SUPER_REPORT, value)
    }

    fun setPanelOrientation(rotation: Int) {
        Log.i(TAG, "setPanelOrientation: $rotation")
        setModeValue(TOUCH_PANEL_ORIENTATION, rotation)
    }
}
