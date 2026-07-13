/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.content.Context

class GamePrefs(context: Context) {

    private val prefs =
        context
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var enabledPackages: Set<String>
        get() = prefs.getStringSet(KEY_ENABLED, emptySet())!!.toSet()
        private set(value) = prefs.edit().putStringSet(KEY_ENABLED, value).apply()

    var customPackages: Set<String>
        get() = prefs.getStringSet(KEY_CUSTOM, emptySet())!!.toSet()
        private set(value) = prefs.edit().putStringSet(KEY_CUSTOM, value).apply()

    fun isEnabled(pkg: String) = enabledPackages.contains(pkg)

    fun setEnabled(pkg: String, enabled: Boolean) {
        enabledPackages =
            enabledPackages.toMutableSet().apply { if (enabled) add(pkg) else remove(pkg) }
    }

    fun addCustom(pkg: String) {
        customPackages = customPackages.toMutableSet().apply { add(pkg) }
    }

    companion object {
        private const val NAME = "game_mode"
        private const val KEY_ENABLED = "enabled_packages"
        private const val KEY_CUSTOM = "custom_packages"
    }
}
