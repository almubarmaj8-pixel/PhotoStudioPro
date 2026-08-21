package com.photostudio.pro.ui.editor

import android.graphics.Typeface

/**
 * طبقة تُضاف فوق الصورة (نص أو ملصق). تُخزَّن بإحداثيات مُطبَّعة (0..1) نسبةً
 * إلى حاوية المعاينة، ما يجعلها مستقلة عن حجم الشاشة وقابلة للرسم بدقة الصورة
 * الكاملة عند التصدير.
 */
sealed class OverlayItem {
    /** مركز الطبقة أفقياً (0..1 نسبةً لعرض حاوية المعاينة). */
    abstract val centerX: Float

    /** مركز الطبقة رأسياً (0..1 نسبةً لارتفاع حاوية المعاينة). */
    abstract val centerY: Float

    /** حجم العنصر بالبكسل كما يظهر في حاوية المعاينة. */
    abstract val sizePx: Float

    data class Text(
        val text: String,
        val color: Int,
        val typeface: Typeface?,
        override val centerX: Float,
        override val centerY: Float,
        override val sizePx: Float
    ) : OverlayItem()

    data class Sticker(
        val emoji: String,
        override val centerX: Float,
        override val centerY: Float,
        override val sizePx: Float
    ) : OverlayItem()
}
