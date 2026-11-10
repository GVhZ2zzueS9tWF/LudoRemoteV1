package com.zippyloo.helper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class TapAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }

    fun tapNormalized(x: Float, y: Float): Boolean {
        if (x !in 0f..1f || y !in 0f..1f) return false
        val dm = resources.displayMetrics
        val path = Path().apply { moveTo(x * dm.widthPixels, y * dm.heightPixels) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80)).build()
        return dispatchGesture(gesture, null, null)
    }
    companion object { @Volatile var instance: TapAccessibilityService? = null }
}
