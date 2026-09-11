/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: GuidixX
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class GameModeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        TouchFeatureManager.attach(this)
        updateUI()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        TouchFeatureManager.attach(this)
        val currentlyActive = TouchFeatureManager.isGameModeActive()
        val newState = !currentlyActive

        if (newState) {
            GameModeService.manualOverride = true
            TouchFeatureManager.setGameMode(true)
        } else {
            GameModeService.manualOverride = false
            TouchFeatureManager.setGameMode(false)
        }

        startService(Intent(this, GameModeService::class.java))
        updateUI()
    }

    private fun updateUI() {
        val tile = qsTile ?: return
        val enabled = TouchFeatureManager.isGameModeActive()
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (enabled) getString(R.string.app_enabled) else null
        tile.updateTile()
    }
}
