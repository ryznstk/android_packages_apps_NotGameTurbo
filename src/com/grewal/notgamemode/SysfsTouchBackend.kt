/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.util.Log
import java.io.File

object SysfsTouchBackend : TouchBackend {

    private const val TAG = "GameModeSysfsBackend"

    private const val BASE = "/sys/class/touch/touch_dev"
    private const val NODE_FILTERS = "$BASE/touch_filters"
    private const val NODE_GAME_MODE = "$BASE/game_mode"
    private const val NODE_SAMPLE_RATE = "$BASE/bump_sample_rate"

    private val FILTER_IDS =
        mapOf(
            TouchFeatureManager.TOUCH_UP_THRESHOLD to 0,
            TouchFeatureManager.TOUCH_TOLERANCE to 1,
            TouchFeatureManager.TOUCH_AIM_SENSITIVITY to 2,
            TouchFeatureManager.TOUCH_TAP_STABILITY to 3,
            TouchFeatureManager.TOUCH_EDGE_FILTER to 4,
            TouchFeatureManager.TOUCH_PANEL_ORIENTATION to 5,
            TouchFeatureManager.TOUCH_EXPERT_MODE to 6,
        )

    private val FILTER_ROW = Regex("""\[(\d+)]:\s*(-?\d+)""")

    override val id = Backend.SYSFS

    override fun isAvailable(): Boolean =
        runCatching { File(NODE_FILTERS).canWrite() && File(NODE_GAME_MODE).canWrite() }
            .onFailure { e -> Log.e(TAG, "failed to probe touch nodes", e) }
            .getOrDefault(false)

    override fun setModeValue(mode: Int, value: Int) {
        when (mode) {
            TouchFeatureManager.TOUCH_GAME_MODE -> write(NODE_GAME_MODE, value.toString())
            TouchFeatureManager.TOUCH_SUPER_REPORT -> write(NODE_SAMPLE_RATE, value.toString())
            else -> {
                val filter =
                    FILTER_IDS[mode]
                        ?: run {
                            Log.w(TAG, "mode $mode has no sysfs node, ignoring")
                            return
                        }
                write(NODE_FILTERS, "$filter $value")
            }
        }
    }

    override fun queryMode(mode: Int): TouchFeatureManager.ModeQuery {
        val cur =
            when (mode) {
                TouchFeatureManager.TOUCH_GAME_MODE -> readInt(NODE_GAME_MODE)
                TouchFeatureManager.TOUCH_SUPER_REPORT -> readInt(NODE_SAMPLE_RATE)
                else -> FILTER_IDS[mode]?.let { readFilter(it) }
            }
        val range =
            TouchFeatureManager.TUNING_RANGES[mode]
                ?: TouchFeatureManager.EXPERT_RANGE.takeIf {
                    mode == TouchFeatureManager.TOUCH_EXPERT_MODE
                }
        return TouchFeatureManager.ModeQuery(
            cur = cur,
            def = range?.def,
            min = range?.min,
            max = range?.max,
            values = null,
        )
    }

    private fun readFilter(filter: Int): Int? =
        read(NODE_FILTERS)
            ?.lineSequence()
            ?.mapNotNull { FILTER_ROW.find(it) }
            ?.firstOrNull { it.groupValues[1].toIntOrNull() == filter }
            ?.groupValues
            ?.get(2)
            ?.toIntOrNull()

    private fun readInt(path: String): Int? = read(path)?.trim()?.toIntOrNull()

    private fun read(path: String): String? =
        runCatching { File(path).readText() }
            .onFailure { e -> Log.e(TAG, "failed to read $path", e) }
            .getOrNull()

    private fun write(path: String, value: String): Boolean =
        runCatching { File(path).writeText(value) }
            .onSuccess { Log.i(TAG, "wrote \"$value\" to $path") }
            .onFailure { e -> Log.e(TAG, "failed to write \"$value\" to $path", e) }
            .isSuccess
}
