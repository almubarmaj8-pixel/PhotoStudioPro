package com.photostudio.pro.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.min

/**
 * يرسم الصورة النهائية بدقة الصورة الأصلية الكاملة (وليس دقة الشاشة)،
 * بما في ذلك طبقات النصوص والملصقات.
 *
 * الإحداثيات المخزّنة في [OverlayItem] مُطبَّعة (0..1) نسبةً إلى حاوية المعاينة،
 * وتُترجم هنا إلى إحداثيات الصورة عبر حساب مستطيل "fitCenter" الفعلي للصورة
 * داخل الحاوية، مع قصّ أي عنصر خارج حدود الصورة.
 */
object ImageExporter {

    fun render(
        bitmap: Bitmap,
        overlays: List<OverlayItem>,
        containerWidth: Int,
        containerHeight: Int
    ): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        if (containerWidth <= 0 || containerHeight <= 0 || overlays.isEmpty()) return out

        val content = fitCenterRect(out.width, out.height, containerWidth, containerHeight)
        if (content.width() <= 0f || content.height() <= 0f) return out
        val scale = out.width / content.width()

        for (overlay in overlays) {
            val cxContainer = overlay.centerX * containerWidth
            val cyContainer = overlay.centerY * containerHeight
            val cx = cxContainer.coerceIn(content.left, content.right)
            val cy = cyContainer.coerceIn(content.top, content.bottom)
            val bx = (cx - content.left) * scale
            val by = (cy - content.top) * scale
            val sizePx = overlay.sizePx * scale

            when (overlay) {
                is OverlayItem.Text -> drawText(canvas, overlay, bx, by, sizePx)
                is OverlayItem.Sticker -> drawEmoji(canvas, overlay, bx, by, sizePx)
            }
        }
        return out
    }

    private fun drawText(canvas: Canvas, item: OverlayItem.Text, bx: Float, by: Float, sizePx: Float) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.color = item.color
        paint.textSize = sizePx
        item.typeface?.let { paint.typeface = it }
        paint.setShadowLayer(sizePx * 0.08f, sizePx * 0.03f, sizePx * 0.03f, Color.BLACK)

        val layout = StaticLayout.Builder
            .obtain(item.text, 0, item.text.length, paint, Int.MAX_VALUE)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(bx - layout.width / 2f, by - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawEmoji(canvas: Canvas, item: OverlayItem.Sticker, bx: Float, by: Float, sizePx: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = sizePx
        paint.textAlign = Paint.Align.CENTER
        val fm = paint.fontMetrics
        val baseline = by - (fm.ascent + fm.descent) / 2f
        canvas.drawText(item.emoji, bx, baseline, paint)
    }

    private fun fitCenterRect(bw: Int, bh: Int, vw: Int, vh: Int): RectF {
        val s = min(vw / bw.toFloat(), vh / bh.toFloat())
        val w = bw * s
        val h = bh * s
        val l = (vw - w) / 2f
        val t = (vh - h) / 2f
        return RectF(l, t, l + w, t + h)
    }
}
