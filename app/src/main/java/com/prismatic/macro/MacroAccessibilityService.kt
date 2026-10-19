package com.prismatic.macro

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.os.Handler
import android.os.Looper

object MacroState {
    var isMacroEnabled = false
    var isServiceRunning = false
}

class MacroAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isHoldingKey = false
    private var windowManager: WindowManager? = null
    private var dotView: DotView? = null
    private var params: WindowManager.LayoutParams? = null
    
    private var targetX = 500f
    private var targetY = 500f

    private val spamRunnable = object : Runnable {
        override fun run() {
            if (MacroState.isMacroEnabled && isHoldingKey) {
                val path = Path()
                path.moveTo(targetX, targetY) 

                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
                    .build()

                dispatchGesture(gesture, null, null)
                handler.postDelayed(this, 15) 
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        MacroState.isServiceRunning = true
        setupDot()
    }

    private fun setupDot() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Make the view 50x50 for the dot
        params = WindowManager.LayoutParams(
            50, 50, 
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_NOT_TOUCHABLE allows clicks to pass through
            // FLAG_NOT_FOCUSABLE stops it from taking over the keyboard
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 500
            y = 500
            targetX = 500f
            targetY = 500f
        }

        dotView = DotView(this).apply {
            // Listen for mouse hover events directly on the view
            setOnHoverListener { view, event ->
                if (event.action == MotionEvent.ACTION_HOVER_MOVE) {
                    targetX = event.rawX
                    targetY = event.rawY

                    params?.x = event.rawX.toInt() - 25
                    params?.y = event.rawY.toInt() - 25
                    
                    windowManager?.updateViewLayout(view, params)
                }
                true
            }
        }
        windowManager?.addView(dotView, params)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!MacroState.isMacroEnabled) return super.onKeyEvent(event)

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            isHoldingKey = true
            handler.post(spamRunnable)
        } else if (event.action == KeyEvent.ACTION_UP) {
            isHoldingKey = false
            handler.removeCallbacks(spamRunnable)
        }
        return true 
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { handler.removeCallbacks(spamRunnable) }
    
    override fun onDestroy() {
        super.onDestroy()
        if (dotView != null) {
            windowManager?.removeView(dotView)
        }
    }
}

// --- THE NEW DOT CROSSHAIR ---
class DotView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.parseColor("#39FF14") // Neon Green
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#39FF14")) 
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw a small dot right in the center
        canvas.drawCircle(width / 2f, height / 2f, 8f, paint)
    }
}
