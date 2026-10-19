package com.prismatic.macro

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.os.Handler
import android.os.Looper

object MacroState {
    var isMacroEnabled = false
    var isServiceRunning = false
}

class MacroAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var heldKeyCode = -1

    private val spamRunnable = object : Runnable {
        override fun run() {
            if (MacroState.isMacroEnabled && heldKeyCode != -1) {
                val downTime = android.os.SystemClock.uptimeMillis()
                val eventDown = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, heldKeyCode, 0)
                val eventUp = KeyEvent(downTime, downTime, KeyEvent.ACTION_UP, heldKeyCode, 0)
                
                try {
                    val inputManager = getSystemService(INPUT_SERVICE) as android.hardware.input.InputManager
                    val injectMethod = inputManager.javaClass.getMethod("injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType)
                    injectMethod.invoke(inputManager, eventDown, 0)
                    injectMethod.invoke(inputManager, eventUp, 0)
                } catch (e: Exception) { }

                handler.postDelayed(this, 15) 
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        MacroState.isServiceRunning = true
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!MacroState.isMacroEnabled) return super.onKeyEvent(event)

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            heldKeyCode = event.keyCode
            handler.post(spamRunnable)
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (event.keyCode == heldKeyCode) {
                heldKeyCode = -1
                handler.removeCallbacks(spamRunnable)
            }
        }
        return true 
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { handler.removeCallbacks(spamRunnable) }
}
