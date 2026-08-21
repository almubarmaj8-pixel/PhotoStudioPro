package com.photostudio.pro.ui.editor

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import kotlin.math.abs

/**
 * يربط عنصراً مرئياً (نص/ملصق) بحركات اللمس داخل حاوية المعاينة:
 *  - سحب لنقل العنصر.
 *  - قرصة (Pinch) لتكبير/تصغير حجمه.
 *  - نقرة للاختيار.
 *  - ضغطة مطوّلة للحذف (تُربط عبر setOnLongClickListener خارجياً).
 *
 * يبلّغ الإحداثيات بصيغة مُطبَّعة (0..1) نسبةً إلى [container] كي تبقى صحيحة
 * مهما تغيّر حجم الشاشة، ويُستخدم الفرق النسبي للإحداثيات (وليس raw مباشرة)
 * لتجنّب أخطاء الموضع السابقة.
 */
class OverlayDragController(
    private val container: ViewGroup,
    private val minSize: Float = 12f,
    private val maxSize: Float = 320f
) {
    var onTransform: ((View, centerX: Float, centerY: Float, sizePx: Float) -> Unit)? = null
    var onSelect: ((View) -> Unit)? = null

    fun attach(view: View, sizeProvider: () -> Float, sizeSetter: (Float) -> Unit) {
        view.setOnTouchListener(TouchHandler(view, sizeProvider, sizeSetter))
    }

    private inner class TouchHandler(
        private val view: View,
        private val sizeProvider: () -> Float,
        private val sizeSetter: (Float) -> Unit
    ) : View.OnTouchListener {

        private val slop = ViewConfiguration.get(container.context).scaledTouchSlop
        private val scaleDetector = ScaleGestureDetector(container.context, ScaleListener())
        private var downRawX = 0f
        private var downRawY = 0f
        private var origX = 0f
        private var origY = 0f
        private var moved = false
        private var scaleStartSize = 0f

        private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                scaleStartSize = sizeProvider()
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val newSize = (scaleStartSize * detector.scaleFactor).coerceIn(minSize, maxSize)
                sizeSetter(newSize)
                report()
                return true
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    origX = view.x
                    origY = view.y
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress) {
                        val dx = event.rawX - downRawX
                        val dy = event.rawY - downRawY
                        if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                        if (moved) {
                            view.x = origX + dx
                            view.y = origY + dy
                            report()
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onSelect?.invoke(view)
                    return true
                }
            }
            return true
        }

        private fun report() {
            // قياس فعلي لضمان دقة المركز بعد تغيير الحجم
            view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val w = view.measuredWidth.coerceAtLeast(1)
            val h = view.measuredHeight.coerceAtLeast(1)
            val cx = ((view.x + w / 2f) / container.width).coerceIn(0f, 1f)
            val cy = ((view.y + h / 2f) / container.height).coerceIn(0f, 1f)
            onTransform?.invoke(view, cx, cy, sizeProvider())
        }
    }
}
