package com.slooshfilm.app.ui.player.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import androidx.core.view.GestureDetectorCompat
import androidx.media3.ui.PlayerView

class DoubleTapPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private val gestureDetector: GestureDetectorCompat
    private val textPaint: Paint
    private val animator: ValueAnimator

    // Handler для сброса счетчиков после задержки
    private val resetHandler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable { reset() }

    private var rewindSeconds = 0
    private var forwardSeconds = 0

    private var isRewinding = false
    private var isForwarding = false

    init {
        textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 56f // Увеличено до 56f
            textAlign = Paint.Align.CENTER
            alpha = 0 // Изначально текст полностью прозрачен
        }

        animator = ValueAnimator.ofInt(255, 0).apply {
            duration = 1000 // Увеличено до 1000 мс
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                textPaint.alpha = animation.animatedValue as Int
                invalidate() // Перерисовываем View на каждом кадре
            }
        }

        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Отменяем предыдущую анимацию и запланированный сброс
                animator.cancel()
                resetHandler.removeCallbacks(resetRunnable)

                // Делаем текст полностью видимым перед началом анимации
                textPaint.alpha = 255

                if (e.x < width / 2) {
                    // Нажатие слева: перематываем назад
                    isRewinding = true
                    isForwarding = false
                    forwardSeconds = 0 // Сбрасываем счетчик перемотки вперед
                    rewindSeconds += 10
                    player?.seekBack()
                } else {
                    // Нажатие справа: перематываем вперед
                    isForwarding = true
                    isRewinding = false
                    rewindSeconds = 0 // Сбрасываем счетчик перемотки назад
                    forwardSeconds += 10
                    player?.seekForward()
                }

                // Запускаем анимацию исчезновения
                animator.start()
                // Планируем сброс счетчиков через 1200 мс
                resetHandler.postDelayed(resetRunnable, 1200)
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                performClick()
                return true
            }
        })

        setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        // Рисуем текст, только если он не полностью прозрачен
        if (textPaint.alpha > 0) {
            val text: String
            val x: Float

            if (isRewinding) {
                text = "- $rewindSeconds сек"
                x = width * 0.25f // Левая четверть экрана
            } else if (isForwarding) {
                text = "+ $forwardSeconds сек"
                x = width * 0.75f // Правая четверть экрана
            } else {
                return
            }

            val y = height / 2f
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            canvas.drawText(text, x, y + textBounds.height() / 2, textPaint)
        }
    }

    private fun reset() {
        isRewinding = false
        isForwarding = false
        rewindSeconds = 0
        forwardSeconds = 0
        invalidate() // Перерисовываем, чтобы убрать последний кадр текста
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Убираем все запланированные действия при уничтожении View
        resetHandler.removeCallbacks(resetRunnable)
        animator.cancel()
    }
}