/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settingslib.widget.SettingsBasePreferenceFragment

class GameModeFragment : SettingsBasePreferenceFragment() {

    private lateinit var prefs: GamePrefs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        prefs = GamePrefs(requireContext())
        rebuild()
    }

    override fun onResume() {
        super.onResume()

        if (::prefs.isInitialized) {
            rebuild()
        }
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

        screen.addPreference(
            Preference(context).apply {
                title = getString(R.string.touch_test_title)
                summary = getString(R.string.touch_test_summary)
                setOnPreferenceClickListener {
                    startActivity(Intent(context, TouchTestActivity::class.java))
                    true
                }
            }
        )

        screen.addPreference(
            Preference(context).apply {
                title = getString(R.string.debug_title)
                summary = getString(R.string.debug_summary)
                setOnPreferenceClickListener {
                    startActivity(Intent(context, DebugActivity::class.java))
                    true
                }
            }
        )

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
            detected.sortedBy { label(pm, it) }.forEach { games.addPreference(appRow(it)) }
        }

        val customCategory =
            PreferenceCategory(context).apply { title = getString(R.string.custom_category_title) }
        screen.addPreference(customCategory)
        custom
            .mapNotNull { runCatching { pm.getApplicationInfo(it, 0) }.getOrNull() }
            .sortedBy { label(pm, it) }
            .forEach { customCategory.addPreference(appRow(it)) }
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

    private fun appRow(app: ApplicationInfo): Preference {
        val pm = requireContext().packageManager
        return Preference(requireContext()).apply {
            title = label(pm, app)
            summary =
                getString(
                    if (prefs.isEnabled(app.packageName)) R.string.app_enabled
                    else R.string.app_disabled
                )
            icon = pm.getApplicationIcon(app)
            setOnPreferenceClickListener {
                startActivity(
                    Intent(requireContext(), AppSettingsActivity::class.java)
                        .putExtra(AppSettingsActivity.EXTRA_PACKAGE, app.packageName)
                )
                true
            }
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

        val iconSize = (32 * resources.displayMetrics.density).toInt()
        val iconPadding = (12 * resources.displayMetrics.density).toInt()
        val adapter =
            object :
                ArrayAdapter<ApplicationInfo>(
                    context,
                    android.R.layout.select_dialog_item,
                    android.R.id.text1,
                    candidates,
                ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val app = candidates[position]
                    val text = view.findViewById<TextView>(android.R.id.text1)
                    text.text = label(pm, app)
                    val icon =
                        pm.getApplicationIcon(app).apply { setBounds(0, 0, iconSize, iconSize) }
                    text.setCompoundDrawablesRelative(icon, null, null, null)
                    text.compoundDrawablePadding = iconPadding
                    return view
                }
            }

        AlertDialog.Builder(context)
            .setTitle(R.string.add_app_dialog_title)
            .setAdapter(adapter) { _, which ->
                prefs.addCustom(candidates[which].packageName)
                rebuild()
            }
            .show()
    }

    private fun label(pm: PackageManager, app: ApplicationInfo) =
        pm.getApplicationLabel(app).toString()
}
