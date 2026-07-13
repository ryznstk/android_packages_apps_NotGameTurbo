/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class GameModeFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: GamePrefs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        prefs = GamePrefs(requireContext())
        rebuild()
    }

    private fun rebuild() {
        val context = requireContext()
        val pm = context.packageManager
        val screen = preferenceManager.createPreferenceScreen(context)

        val custom = prefs.customPackages
        val detected =
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0)).filter {
                it.category == ApplicationInfo.CATEGORY_GAME && it.packageName !in custom
            }

        val games =
            PreferenceCategory(context).apply { title = getString(R.string.games_category_title) }
        screen.addPreference(games)
        if (detected.isEmpty()) {
            games.addPreference(
                Preference(context).apply {
                    summary = getString(R.string.no_games_summary)
                    isSelectable = false
                }
            )
        } else {
            detected.sortedBy { label(pm, it) }.forEach { games.addPreference(appSwitch(it)) }
        }

        val customCategory =
            PreferenceCategory(context).apply { title = getString(R.string.custom_category_title) }
        screen.addPreference(customCategory)
        custom
            .mapNotNull { runCatching { pm.getApplicationInfo(it, 0) }.getOrNull() }
            .sortedBy { label(pm, it) }
            .forEach { customCategory.addPreference(appSwitch(it)) }
        customCategory.addPreference(
            Preference(context).apply {
                title = getString(R.string.add_app_title)
                setOnPreferenceClickListener {
                    showAppPicker()
                    true
                }
            }
        )

        preferenceScreen = screen
    }

    private fun appSwitch(app: ApplicationInfo) =
        SwitchPreferenceCompat(requireContext()).apply {
            val pm = requireContext().packageManager
            title = label(pm, app)
            summary = getString(R.string.app_summary)
            icon = pm.getApplicationIcon(app)
            isChecked = prefs.isEnabled(app.packageName)
            setOnPreferenceChangeListener { _, newValue ->
                prefs.setEnabled(app.packageName, newValue as Boolean)
                true
            }
        }

    private fun showAppPicker() {
        val context = requireContext()
        val pm = context.packageManager
        val shown =
            prefs.customPackages +
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                    .filter { it.category == ApplicationInfo.CATEGORY_GAME }
                    .map { it.packageName }

        val candidates =
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                .filter {
                    it.packageName !in shown && pm.getLaunchIntentForPackage(it.packageName) != null
                }
                .sortedBy { label(pm, it) }

        val labels = candidates.map { label(pm, it) }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.add_app_dialog_title)
            .setItems(labels) { _, which ->
                prefs.addCustom(candidates[which].packageName)
                rebuild()
            }
            .show()
    }

    private fun label(pm: PackageManager, app: ApplicationInfo) =
        pm.getApplicationLabel(app).toString()
}
