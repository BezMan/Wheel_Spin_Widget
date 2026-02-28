package com.bez.spinwheel_sdk.presentation.widget

import android.content.Context
import androidx.core.content.edit

/** Persists widget-side rotation and spinning flag across widget updates. */
internal class WidgetState(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRotation(): Float = prefs.getFloat(KEY_ROTATION, 0f)
    fun setRotation(v: Float) = prefs.edit { putFloat(KEY_ROTATION, v) }

    fun isSpinning(): Boolean = prefs.getBoolean(KEY_SPINNING, false)
    fun setSpinning(v: Boolean) = prefs.edit { putBoolean(KEY_SPINNING, v) }

    /** True while the in-app spin animation is running. Used to show a placeholder on the widget. */
    fun isAppSpinning(): Boolean = prefs.getBoolean(KEY_APP_SPINNING, false)
    fun setAppSpinning(v: Boolean) = prefs.edit { putBoolean(KEY_APP_SPINNING, v) }

    companion object {
        internal const val PREFS_NAME = "spinwheel_widget_state"
        internal const val KEY_ROTATION = "rotation"
        private const val KEY_SPINNING = "spinning"
        private const val KEY_APP_SPINNING = "app_spinning"
    }
}
