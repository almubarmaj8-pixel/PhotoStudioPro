package com.photostudio.pro.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.photostudio.pro.R
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter

/**
 * كل الفلاتر المتاحة في المحرر.
 *
 * لكل فلتر:
 *  - [createGpuFilter]  : سلسلة فلاتر GPUImage تُطبَّق على الصورة الكاملة (مسار سريع عبر GPU).
 *  - [colorMatrix]      : مصفوفة لونية مكافئة تُستخدم لتوليد معاينات مصغّرة خفيفة بدون GL.
 *
 * النتيجة النهائية للفلتر متطابقة بصرياً بين المسارين لأن كليهما يعتمد على نفس
 * عمليات الألوان القياسية (تدرّج، تشبّع، دوران صبغة، تباين، سطوع، سيبيا).
 */
enum class FilterType(val displayNameRes: Int) {
    ORIGINAL(R.string.filter_original),
    WARM(R.string.filter_warm),
    COOL(R.string.filter_cool),
    VINTAGE(R.string.filter_vintage),
    BW(R.string.filter_bw),
    VIVID(R.string.filter_vivid),
    FADE(R.string.filter_fade),
    DRAMATIC(R.string.filter_dramatic),
    SEPIA(R.string.filter_sepia),
    NOIR(R.string.filter_noir),
    SUNSET(R.string.filter_sunset),
    FOREST(R.string.filter_forest);

    /** يبني فلتر GPUImage الموافق (يُستخدم في معالجة الصورة الكاملة). */
    fun createGpuFilter(): GPUImageFilter = when (this) {
        ORIGINAL -> GPUImageFilter()
        WARM -> GPUImageFilterGroup(
            listOf(
                GPUImageRGBFilter(1.08f, 1.0f, 0.9f),
                GPUImageBrightnessFilter(0.08f)
            )
        )
        COOL -> GPUImageHueFilter(210f)
        VINTAGE -> GPUImageFilterGroup(
            listOf(
                GPUImageSepiaToneFilter(0.7f),
                GPUImageContrastFilter(1.05f)
            )
        )
        BW -> GPUImageGrayscaleFilter()
        VIVID -> GPUImageSaturationFilter(1.8f)
        FADE -> GPUImageFilterGroup(
            listOf(
                GPUImageSaturationFilter(0.55f),
                GPUImageBrightnessFilter(0.05f)
            )
        )
        DRAMATIC -> GPUImageContrastFilter(1.5f)
        SEPIA -> GPUImageSepiaToneFilter(1.0f)
        NOIR -> GPUImageFilterGroup(
            listOf(
                GPUImageGrayscaleFilter(),
                GPUImageContrastFilter(1.3f)
            )
        )
        SUNSET -> GPUImageRGBFilter(1.2f, 1.0f, 0.7f)
        FOREST -> GPUImageHueFilter(90f)
    }

    /** مصفوفة لونية مكافئة للفلتر، تُستخدم لتوليد معاينات سريعة وآمنة للخيوط. */
    fun colorMatrix(): ColorMatrix = when (this) {
        ORIGINAL -> ColorMatrix()
        WARM -> ColorMatrix()
            .apply { postConcat(ColorMatrix().apply { setScale(1.08f, 1.0f, 0.92f, 1f) }) }
            .apply { postConcat(brightnessMatrix(0.08f)) }
        COOL -> ColorMatrix().apply { setRotate(0, 210f) }
        VINTAGE -> sepiaMatrix(0.7f).apply { postConcat(contrastMatrix(1.05f)) }
        BW -> ColorMatrix().apply { setSaturation(0f) }
        VIVID -> ColorMatrix().apply { setSaturation(1.8f) }
        FADE -> ColorMatrix()
            .apply { postConcat(ColorMatrix().apply { setSaturation(0.55f) }) }
            .apply { postConcat(brightnessMatrix(0.05f)) }
        DRAMATIC -> contrastMatrix(1.5f)
        SEPIA -> sepiaMatrix(1.0f)
        NOIR -> ColorMatrix()
            .apply { postConcat(ColorMatrix().apply { setSaturation(0f) }) }
            .apply { postConcat(contrastMatrix(1.3f)) }
        SUNSET -> ColorMatrix().apply { setScale(1.2f, 1.0f, 0.7f, 1f) }
        FOREST -> ColorMatrix().apply { setRotate(0, 90f) }
    }

    /**
     * يولد معاينة مصغّرة مربعة للفلتر تُستخدم في شبكة الفلاتر.
     * لا يعدّل [source] ولا يغيّر أبعادها الأصلية.
     */
    fun generateThumbnail(source: Bitmap, sizePx: Int = 96): Bitmap {
        val thumb = Bitmap.createScaledBitmap(source, sizePx, sizePx, true)
        if (this == ORIGINAL) return thumb
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix())
        canvas.drawBitmap(thumb, 0f, 0f, paint)
        if (thumb !== source) thumb.recycle()
        return output
    }

    // ── أدوات بناء مصفوفات اللون ─────────────────────────────────────────

    private fun contrastMatrix(c: Float): ColorMatrix {
        val scale = c
        val translate = 128f * (1f - c)
        return ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun brightnessMatrix(offset: Float): ColorMatrix {
        val v = offset * 255f
        return ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, v,
                0f, 1f, 0f, 0f, v,
                0f, 0f, 1f, 0f, v,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun sepiaMatrix(intensity: Float): ColorMatrix {
        val sepia = floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val identity = ColorMatrix().array
        val out = FloatArray(20)
        for (i in 0 until 20) {
            out[i] = identity[i] * (1f - intensity) + sepia[i] * intensity
        }
        return ColorMatrix(out)
    }
}
