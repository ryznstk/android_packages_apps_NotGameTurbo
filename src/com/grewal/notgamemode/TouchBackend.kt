/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

enum class Backend {
    HAL,
    SYSFS,
}

interface TouchBackend {
    val id: Backend
    fun isAvailable(): Boolean
    fun setModeValue(mode: Int, value: Int)
    fun queryMode(mode: Int): TouchFeatureManager.ModeQuery
}
