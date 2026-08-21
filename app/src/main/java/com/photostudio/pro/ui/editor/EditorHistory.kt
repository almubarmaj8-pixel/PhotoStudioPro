package com.photostudio.pro.ui.editor

import android.graphics.Bitmap

/**
 * نظام تراجع/إعادة (Undo/Redo) مبني على لقطات حالة المحرر.
 *
 * كل لقطة تحفظ نسخة من الصورة الأساسية (بعد التحويلات الهندسية)، وقائمة الطبقات،
 * والفلتر الحالي وقيم التعديلات. عند تجاوز الحد الأقصى تُعاد تدوير أقدم لقطة
 * (recycle) لتفادي تسريب الذاكرة.
 */
class EditorHistory(private val maxSize: Int = 15) {

    data class Snapshot(
        val bitmap: Bitmap,
        val overlays: List<OverlayItem>,
        val filter: FilterType,
        val brightness: Float,
        val contrast: Float,
        val saturation: Float,
        val warmth: Float,
        val sharpness: Float
    )

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** يدفع لقطة جديدة إلى مكدس التراجع ويمسح الإعادة. */
    fun push(snapshot: Snapshot) {
        undoStack.addLast(snapshot)
        while (undoStack.size > maxSize) {
            recycle(undoStack.removeFirst())
        }
        clearRedo()
    }

    /** يسحب آخر لقطة من التراجع، ويعيد الحالة الحالية إلى مكدس الإعادة. */
    fun undo(current: Snapshot): Snapshot? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    /** يسحب لقطة من الإعادة، ويعيد الحالة الحالية إلى مكدس التراجع. */
    fun redo(current: Snapshot): Snapshot? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return next
    }

    fun clearRedo() {
        while (redoStack.isNotEmpty()) recycle(redoStack.removeLast())
    }

    fun clearAll() {
        while (undoStack.isNotEmpty()) recycle(undoStack.removeLast())
        clearRedo()
    }

    private fun recycle(snapshot: Snapshot) {
        if (!snapshot.bitmap.isRecycled) snapshot.bitmap.recycle()
    }
}
