package com.photostudio.pro.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.photostudio.pro.R

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
    FOREST(R.string.filter_forest)
}

fun FilterType.generatePreview(source: Bitmap): Bitmap {
    val preview = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(preview)
    val paint = Paint().apply { color = Color.WHITE }
    canvas.drawRect(0f, 0f, 72f, 72f, paint)
    return preview
}
