package com.slooshfilm.app.ui.utils

import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

fun View.applyScaleAnimation() {
    this.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }
        false
    }
}
