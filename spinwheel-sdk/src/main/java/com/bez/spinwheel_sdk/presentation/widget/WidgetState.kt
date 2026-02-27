package com.bez.spinwheel_sdk.presentation.widget

import android.content.Context

/** Persists widget-side rotation and spinning flag across widget updates. */
internal class WidgetState(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRotation(): Float = prefs.getFloat(KEY_ROTATION, 0f)
    fun setRotation(v: Float) = prefs.edit().putFloat(KEY_ROTATION, v).apply()

    fun isSpinning(): Boolean = prefs.getBoolean(KEY_SPINNING, false)
    fun setSpinning(v: Boolean) = prefs.edit().putBoolean(KEY_SPINNING, v).apply()

    companion object {
        private const val PREFS_NAME = "spinwheel_widget_state"
        private const val KEY_ROTATION = "rotation"
        private const val KEY_SPINNING = "spinning"
    }
}
