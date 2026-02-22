package com.kindlevibe.reader.ui.utils

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun ImmersiveMode(active: Boolean) {
    val view = LocalView.current

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowInsetsControllerCompat(window, view)

        if (active) {
            controller.hide(
                WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.navigationBars()
            )
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(
                WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.navigationBars()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? Activity)?.window ?: return@onDispose
            val controller = WindowInsetsControllerCompat(window, view)
            controller.show(
                WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.navigationBars()
            )
        }
    }
}
