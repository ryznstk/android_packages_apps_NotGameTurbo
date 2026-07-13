/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log

class GameModeService : Service() {

    private lateinit var prefs: GamePrefs
    private lateinit var usageStats: UsageStatsManager
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler

    private var lastForeground: String? = null
    private var gameModeActive = false

    private val poll =
        object : Runnable {
            override fun run() {
                update()
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }

    override fun onCreate() {
        super.onCreate()
        prefs = GamePrefs(this)
        usageStats = getSystemService(UsageStatsManager::class.java)
        thread = HandlerThread(TAG).apply { start() }
        handler = Handler(thread.looper)
        handler.post(poll)
        Log.i(TAG, "started")
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        thread.quitSafely()
        if (gameModeActive) {
            TouchFeatureManager.setGameMode(false)
            gameModeActive = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun update() {

        if (manualOverride) {
            return
        }

        val foreground = currentForegroundPackage() ?: lastForeground
        lastForeground = foreground

        val shouldEnable = foreground != null && prefs.isEnabled(foreground)
        if (shouldEnable != gameModeActive) {
            gameModeActive = shouldEnable
            TouchFeatureManager.setGameMode(shouldEnable)
        }
    }

    private fun currentForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(now - QUERY_WINDOW_MS, now)
        val event = android.app.usage.UsageEvents.Event()
        var pkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (
                event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                pkg = event.packageName
            }
        }
        return pkg
    }

    companion object {
        private const val TAG = "GameModeService"
        private const val POLL_INTERVAL_MS = 2000L
        private const val QUERY_WINDOW_MS = 10_000L

        @Volatile var manualOverride = false
    }
}
