/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.notgamemode

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

class TouchTestView(context: Context) : View(context) {

    private val windowSamples = ArrayDeque<Long>()
    private val recentDeltas = ArrayDeque<Long>()
    private var lastSampleTime = 0L

    private var instantRate = 0
    private var avgRate = 0
    private var peakRate = 0
    private var touchX = -1f
    private var touchY = -1f
    private var tracking = false

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val ratePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GREEN
            textSize = 150f
            isFakeBoldText = true
        }
    private val infoPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
        }
    private val crossPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.CYAN
            strokeWidth = 3f
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                reset()
                consume(event.eventTime)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    consume(event.getHistoricalEventTime(i))
                }
                consume(event.eventTime)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> tracking = false
        }
        touchX = event.x
        touchY = event.y
        recompute(event.eventTime)
        invalidate()
        return true
    }

    private fun reset() {
        tracking = true
        windowSamples.clear()
        recentDeltas.clear()
        lastSampleTime = 0L
        peakRate = 0
    }

    private fun consume(time: Long) {
        if (lastSampleTime in 1 until time) {
            val delta = time - lastSampleTime

            if (delta in MIN_DELTA_MS..MAX_DELTA_MS) {
                recentDeltas.addLast(delta)
                while (recentDeltas.size > SMOOTH_SAMPLES) recentDeltas.removeFirst()
            }
        }
        lastSampleTime = time
        windowSamples.addLast(time)
    }

    private fun recompute(now: Long) {
        while (windowSamples.isNotEmpty() && now - windowSamples.first() > WINDOW_MS) {
            windowSamples.removeFirst()
        }
        avgRate = windowSamples.size

        val deltaSum = recentDeltas.sum()
        instantRate = if (deltaSum > 0) (1000L * recentDeltas.size / deltaSum).toInt() else 0
        if (tracking && instantRate > peakRate) peakRate = instantRate
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (tracking && touchX >= 0f) {
            canvas.drawLine(touchX, 0f, touchX, height.toFloat(), crossPaint)
            canvas.drawLine(0f, touchY, width.toFloat(), touchY, crossPaint)
        }

        canvas.drawText("$instantRate Hz", 60f, 230f, ratePaint)
        canvas.drawText("peak: $peakRate Hz    avg(1s): $avgRate Hz", 60f, 310f, infoPaint)
        canvas.drawText(
            if (tracking) "Keep dragging your finger…" else "Touch and drag to measure",
            60f,
            height - 90f,
            infoPaint,
        )
    }

    companion object {
        private const val WINDOW_MS = 1000L
        private const val MIN_DELTA_MS = 1L
        private const val MAX_DELTA_MS = 100L
        private const val SMOOTH_SAMPLES = 8
    }
}
