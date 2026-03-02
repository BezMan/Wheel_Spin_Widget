package com.bez.spinwheel_sdk.presentation.widget

import android.content.Context
import androidx.core.content.edit

/** Persists widget-side rotation and spinning flags across widget updates. */
internal class WidgetState(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRotation(): Float = prefs.getFloat(KEY_ROTATION, 0f)
    fun setRotation(v: Float) = prefs.edit { putFloat(KEY_ROTATION, v) }

    /** True while the home-screen widget animation is running. */
    fun isWidgetSpinning(): Boolean = prefs.getBoolean(KEY_WIDGET_SPINNING, false)
    fun setWidgetSpinning(v: Boolean) = prefs.edit { putBoolean(KEY_WIDGET_SPINNING, v) }

    /** True while the in-app spin animation is running. Used to show a placeholder on the widget. */
    fun isAppSpinning(): Boolean = prefs.getBoolean(KEY_APP_SPINNING, false)
    fun setAppSpinning(v: Boolean) = prefs.edit { putBoolean(KEY_APP_SPINNING, v) }

    /** Remaining spin count. Null = not yet seeded from config (no limit applied). */
    fun getSpinsRemaining(): Int? =
        if (prefs.contains(KEY_SPINS_REMAINING)) prefs.getInt(KEY_SPINS_REMAINING, 0) else null
    fun setSpinsRemaining(count: Int) = prefs.edit { putInt(KEY_SPINS_REMAINING, count) }

    companion object {
        internal const val PREFS_NAME = "spinwheel_widget_state"
        internal const val KEY_ROTATION = "rotation"
        private const val KEY_WIDGET_SPINNING = "widget_spinning"
        private const val KEY_APP_SPINNING = "app_spinning"
        private const val KEY_SPINS_REMAINING = "spins_remaining"
    }
}
