package com.sultonuzdev.netspeed.data.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.sultonuzdev.netspeed.R
import com.sultonuzdev.netspeed.utils.FormattedSpeed
import com.sultonuzdev.netspeed.utils.OverlayPermissionHelper
import com.sultonuzdev.netspeed.utils.SpeedDisplayMode
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A draggable always-on-top speed readout.
 *
 * Built in code rather than inflated from XML because it is two text views and a rounded
 * background whose colour, size and opacity are all user-controlled at runtime — an XML layout
 * would have to be re-styled on every preference change anyway.
 *
 * The view is owned by the monitoring service, which already samples the speeds; this class only
 * renders what it is handed.
 */
class SpeedOverlayManager(
    private val context: Context,
    private val onPositionChanged: (x: Int, y: Int) -> Unit,
    /** Invoked on a tap that was not a drag. */
    private val onTap: () -> Unit = {}
) {

    private val windowManager: WindowManager? =
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var rootView: LinearLayout? = null
    private var primaryText: TextView? = null
    private var secondaryText: TextView? = null
    private var detailText: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var background = GradientDrawable()

    /** Last rendered lines, so an unchanged tick does not trigger a layout pass every second. */
    private var lastPrimary: String? = null
    private var lastSecondary: String? = null
    private var lastDetail: String? = null

    val isShowing: Boolean get() = rootView != null

    /**
     * Adds the overlay if it is not already up. Safe to call repeatedly.
     *
     * Returns false when the permission is missing — it is revocable at any time, so it is
     * rechecked here rather than trusted from when the setting was switched on.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show(x: Int, y: Int, textSizeSp: Int, color: Int, opacityPercent: Int): Boolean {
        if (!OverlayPermissionHelper.canDrawOverlays(context)) return false
        val manager = windowManager ?: return false
        if (isShowing) return true

        val primary = TextView(context).apply {
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp.toFloat())
            includeFontPadding = false
            applyContrastShadow(color)
        }
        val secondary = TextView(context).apply {
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp * 0.85f)
            includeFontPadding = false
            applyContrastShadow(color)
        }

        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8f)
            setColor(backgroundColorFor(opacityPercent))
        }

        val detail = TextView(context).apply {
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp * 0.75f)
            includeFontPadding = false
            applyContrastShadow(color)
        }

        // The launcher icon makes it obvious which app the floating readout belongs to, and
        // gives the tap target something to point at.
        val iconSize = dp(textSizeSp * 1.1f).toInt()
        val icon = ImageView(context).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = dp(6f).toInt()
            }
        }

        val lines = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            addView(primary)
            addView(secondary)
            addView(detail)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f).toInt(), dp(5f).toInt(), dp(8f).toInt(), dp(5f).toInt())
            background = this@SpeedOverlayManager.background
            addView(icon)
            addView(lines)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Not focusable, so it never steals input from the app underneath; not touch-modal,
            // so taps outside it still reach that app.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        container.setOnTouchListener(DragListener(params, manager))

        return try {
            manager.addView(container, params)
            rootView = container
            primaryText = primary
            secondaryText = secondary
            detailText = detail
            layoutParams = params
            // The stored position may be off-screen: saved in the other orientation, restored
            // onto a different device, or left there by a drag past the edge. Nothing else would
            // bring it back, so re-clamp once the view has been measured.
            container.post { ensureOnScreen() }
            true
        } catch (e: Exception) {
            // The window can be refused (permission revoked between check and add, OEM policy).
            e.printStackTrace()
            false
        }
    }

    /**
     * Pulls the overlay back inside the display if it currently sits outside it. Safe to call at
     * any time; does nothing when the position is already valid.
     */
    fun ensureOnScreen() {
        val view = rootView ?: return
        val params = layoutParams ?: return
        val clampedX = clampX(params.x, view.width)
        val clampedY = clampY(params.y, view.height)
        if (clampedX == params.x && clampedY == params.y) return

        params.x = clampedX
        params.y = clampedY
        try {
            windowManager?.updateViewLayout(view, params)
            onPositionChanged(clampedX, clampedY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clampX(x: Int, viewWidth: Int): Int {
        val screenWidth = screenSize()?.first ?: return x
        return x.coerceIn(0, (screenWidth - viewWidth).coerceAtLeast(0))
    }

    private fun clampY(y: Int, viewHeight: Int): Int {
        val screenHeight = screenSize()?.second ?: return y
        return y.coerceIn(0, (screenHeight - viewHeight).coerceAtLeast(0))
    }

    private fun screenSize(): Pair<Int, Int>? {
        val manager = windowManager ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = manager.currentWindowMetrics.bounds
                bounds.width() to bounds.height()
            } else {
                @Suppress("DEPRECATION")
                val metrics = DisplayMetrics().also { manager.defaultDisplay.getMetrics(it) }
                metrics.widthPixels to metrics.heightPixels
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hide() {
        val view = rootView ?: return
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        rootView = null
        primaryText = null
        secondaryText = null
        detailText = null
        layoutParams = null
        lastPrimary = null
        lastSecondary = null
        lastDetail = null
    }

    /** Applies preference changes without tearing the window down. */
    fun restyle(textSizeSp: Int, color: Int, opacityPercent: Int) {
        primaryText?.apply {
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp.toFloat())
            applyContrastShadow(color)
        }
        secondaryText?.apply {
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp * 0.85f)
            applyContrastShadow(color)
        }
        detailText?.apply {
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp * 0.75f)
            applyContrastShadow(color)
        }
        background.setColor(backgroundColorFor(opacityPercent))
    }

    /**
     * Outlines the text in its opposite luminance.
     *
     * The overlay floats over arbitrary content and the user can set the background to fully
     * transparent, so nothing else guarantees contrast -- black text at 0% opacity over a dark
     * app would otherwise be invisible.
     */
    private fun TextView.applyContrastShadow(color: Int) {
        val shadow = if (ColorUtils.calculateLuminance(color) > 0.5) Color.BLACK else Color.WHITE
        setShadowLayer(dp(2f), 0f, 0f, ColorUtils.setAlphaComponent(shadow, 180))
    }

    /** Mirrors the notification's display mode so the two never disagree. */
    fun update(
        download: FormattedSpeed,
        upload: FormattedSpeed,
        mode: SpeedDisplayMode,
        /** Latency and session total: the things the notification does not already report. */
        detailLine: String?
    ) {
        val primary = primaryText ?: return
        val secondary = secondaryText ?: return
        val detail = detailText ?: return

        val primaryLine: String
        val secondaryLine: String?
        when (mode) {
            SpeedDisplayMode.BOTH -> {
                primaryLine = "↓ $download"
                secondaryLine = "↑ $upload"
            }

            SpeedDisplayMode.UPLOAD -> {
                primaryLine = "↑ $upload"
                secondaryLine = null
            }

            SpeedDisplayMode.COMBINED, SpeedDisplayMode.DOWNLOAD -> {
                primaryLine = "↓ $download"
                secondaryLine = null
            }
        }

        // Assigning identical text still forces a measure/layout pass, once a second, forever.
        if (primaryLine == lastPrimary &&
            secondaryLine == lastSecondary &&
            detailLine == lastDetail
        ) {
            return
        }
        lastPrimary = primaryLine
        lastSecondary = secondaryLine
        lastDetail = detailLine

        primary.text = primaryLine
        if (secondaryLine == null) {
            secondary.visibility = View.GONE
        } else {
            secondary.text = secondaryLine
            secondary.visibility = View.VISIBLE
        }

        if (detailLine.isNullOrBlank()) {
            detail.visibility = View.GONE
        } else {
            detail.text = detailLine
            detail.visibility = View.VISIBLE
        }
    }

    private fun backgroundColorFor(opacityPercent: Int): Int {
        val alpha = (opacityPercent.coerceIn(0, 100) * 255 / 100)
        return Color.argb(alpha, 0, 0, 0)
    }

    private fun dp(value: Float): Float =
        value * context.resources.displayMetrics.density

    /**
     * Moves the window with the finger, persisting only on release so a drag does not write to
     * DataStore on every touch event.
     */
    private inner class DragListener(
        private val params: WindowManager.LayoutParams,
        private val manager: WindowManager
    ) : View.OnTouchListener {

        private var initialX = 0
        private var initialY = 0
        private var touchStartX = 0f
        private var touchStartY = 0f
        private var dragged = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    dragged = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    if (abs(dx) > TOUCH_SLOP_PX || abs(dy) > TOUCH_SLOP_PX) dragged = true

                    // Without clamping, a drag past the edge put the overlay outside the
                    // display and persisted that position -- it stayed lost even after toggling
                    // the setting off and back on.
                    params.x = clampX(initialX + dx.roundToInt(), view.width)
                    params.y = clampY(initialY + dy.roundToInt(), view.height)
                    try {
                        manager.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragged) {
                        onPositionChanged(params.x, params.y)
                    } else if (event.action == MotionEvent.ACTION_UP) {
                        // Never moved past the slop, so treat it as a tap and open the app.
                        view.performClick()
                        onTap()
                    }
                    return true
                }
            }
            return false
        }
    }

    private companion object {
        const val TOUCH_SLOP_PX = 8f
    }
}
