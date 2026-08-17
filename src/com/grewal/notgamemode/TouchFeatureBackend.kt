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

object TouchFeatureBackend : TouchBackend {

    private const val TAG = "GameModeTouchFeatureBackend"

    private const val TOUCH_ID = 0

    override val id = Backend.HAL

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

    override fun isAvailable(): Boolean = getService() != null

    override fun setModeValue(mode: Int, value: Int) {
        val service =
            getService()
                ?: run {
                    Log.e(TAG, "touchfeature service is null, cannot set mode $mode")
                    return
                }
        runCatching { service.setModeValue(TOUCH_ID, mode, value) }
            .onFailure { e -> Log.e(TAG, "setModeValue(mode=$mode, value=$value) failed", e) }
    }

    override fun queryMode(mode: Int): TouchFeatureManager.ModeQuery {
        val service = getService()
        fun <T> attempt(block: (ITouchFeature) -> T): T? =
            service?.let { runCatching { block(it) }.getOrNull() }
        return TouchFeatureManager.ModeQuery(
            cur = attempt { it.getModeCurValue(TOUCH_ID, mode) },
            def = attempt { it.getModeDefaultValue(TOUCH_ID, mode) },
            min = attempt { it.getModeMinValue(TOUCH_ID, mode) },
            max = attempt { it.getModeMaxValue(TOUCH_ID, mode) },
            values = attempt { it.getModeValue(TOUCH_ID, mode).toList() },
        )
    }
}
