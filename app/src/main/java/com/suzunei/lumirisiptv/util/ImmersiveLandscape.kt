package com.suzunei.lumirisiptv.util

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Locks the activity to landscape and hides system bars (immersive sticky). Safe to call from
 * `onCreate`; survives configuration changes when the activity declares `configChanges`.
 */
fun Activity.applyImmersiveLandscape() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
