package com.photostudio.pro.ui.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

/**
 * إطار اقتصادص تفاعلي مرسوم فوق المعاينة:
 *  - قناع معتم خارج الإطار.
 *  - حد أبيض + شبكة قاعدة الأثلاث.
 *  - سحب لتحريك الإطار، وقرصة لتغيير حجمه.
 *  - دعم نسبة أبعاد ثابتة ([setAspectRatio]) أو حرّة (null).
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val cropRect = RectF()
    private var aspectRatio: Float? = null

    private val dimPaint = Paint().apply {
        color = 0x99000000.toInt()
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1.5f
        isAntiAlias = true
    }
    private val gridPaint = Paint().apply {
        color = 0x66FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 0.8f
    }

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())

    private var downRawX = 0f
    private var downRawY = 0f
    private var startLeft = 0f
    private var startTop = 0f
    private var dragging = false
    private var scaleStartW = 0f
    private var scaleStartH = 0f

    /** يضبط نسبة الأبعاد (مثل 1f، 4f/3f) أو null للحرّ، ويعيد تشكيل الإطار. */
    fun setAspectRatio(ratio: Float?) {
        aspectRatio = ratio
        if (width > 0 && height > 0) {
            val inset = resources.displayMetrics.density * 16f
            val maxW = width - inset * 2
            val maxH = height - inset * 2
            val ratioVal = ratio
            if (ratioVal != null) {
                var w = maxW
                var h = w / ratioVal
                if (h > maxH) {
                    h = maxH
                    w = h * ratioVal
                }
                val left = (width - w) / 2f
                val top = (height - h) / 2f
                cropRect.set(left, top, left + w, top + h)
            } else {
                cropRect.set(inset, inset, width - inset, height - inset)
            }
            invalidate()
        }
    }

    /** يعيد مستطيل الاقتصاص بإحداثيات العرض. */
    fun getCropRect(): RectF = RectF(cropRect)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && cropRect.isEmpty) setAspectRatio(aspectRatio)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cropRect.isEmpty) return

        // قناع معتم حول الإطار
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, cropRect.top, dimPaint)
        canvas.drawRect(0f, cropRect.bottom, w, h, dimPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, dimPaint)
        canvas.drawRect(cropRect.right, cropRect.top, w, cropRect.bottom, dimPaint)

        // الحد والشبكة
        canvas.drawRect(cropRect, borderPaint)
        val thirdW = cropRect.width() / 3f
        val thirdH = cropRect.height() / 3f
        for (i in 1..2) {
            val x = cropRect.left + thirdW * i
            canvas.drawLine(x, cropRect.top, x, cropRect.bottom, gridPaint)
            val y = cropRect.top + thirdH * i
            canvas.drawLine(cropRect.left, y, cropRect.right, y, gridPaint)
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            scaleStartW = cropRect.width()
            scaleStartH = cropRect.height()
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor.coerceIn(0.3f, 3f)
            var newW = scaleStartW * factor
            var newH = scaleStartH * factor
            val ratio = aspectRatio
            if (ratio != null) {
                newW = newW.coerceIn(40f, width.toFloat())
                newH = newW / ratio
                if (newH > height.toFloat()) {
                    newH = height.toFloat()
                    newW = newH * ratio
                }
            } else {
                newW = newW.coerceIn(40f, width.toFloat())
                newH = newH.coerceIn(40f, height.toFloat())
            }
            centerRect(newW, newH)
            invalidate()
            return true
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (cropRect.contains(event.x, event.y)) {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startLeft = cropRect.left
                    startTop = cropRect.top
                    dragging = true
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging && !scaleDetector.isInProgress) {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    val newLeft = (startLeft + dx).coerceIn(0f, width - cropRect.width())
                    val newTop = (startTop + dy).coerceIn(0f, height - cropRect.height())
                    cropRect.offsetTo(newLeft, newTop)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
        }
        return true
    }

    private fun centerRect(newW: Float, newH: Float) {
        val cx = cropRect.centerX()
        val cy = cropRect.centerY()
        var left = cx - newW / 2f
        var top = cy - newH / 2f
        left = left.coerceIn(0f, (width - newW).coerceAtLeast(0f))
        top = top.coerceIn(0f, (height - newH).coerceAtLeast(0f))
        cropRect.set(left, top, left + newW, top + newH)
    }
}
