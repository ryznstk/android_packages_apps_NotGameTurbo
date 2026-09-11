/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: GuidixX
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.widget.Toast

object TileUtils {

    fun requestAddTileService(
        context: Context,
        tileServiceClass: Class<*>,
        labelResId: Int,
        iconResId: Int,
    ) {
        val componentName = ComponentName(context, tileServiceClass)
        val label = context.getString(labelResId)
        val icon = Icon.createWithResource(context, iconResId)
        val sbm = context.getSystemService(Context.STATUS_BAR_SERVICE) as? StatusBarManager

        sbm?.requestAddTileService(
            componentName,
            label,
            icon,
            context.mainExecutor,
        ) { result ->
            handleResult(context, result)
        }
    }

    private fun handleResult(context: Context, result: Int?) {
        if (result == null) return
        when (result) {
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                Toast.makeText(context, R.string.tile_added, Toast.LENGTH_SHORT).show()
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                Toast.makeText(context, R.string.tile_not_added, Toast.LENGTH_SHORT).show()
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                Toast.makeText(context, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
        }
    }
}
