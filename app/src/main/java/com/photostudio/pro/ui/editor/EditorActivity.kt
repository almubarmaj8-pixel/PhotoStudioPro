package com.photostudio.pro.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.photostudio.pro.R
import com.photostudio.pro.databinding.ActivityEditorBinding
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import kotlin.math.min

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val MAX_IMAGE_DIMENSION = 2048
    }

    private lateinit var binding: ActivityEditorBinding
    private lateinit var gpuImage: GPUImage

    /** الصورة الأصلية المقروءة من المصدر (تُحرَّر في onDestroy فقط). */
    private var sourceBitmap: Bitmap? = null

    /** الصورة بعد التحويلات الهندسية (تدوير/قلب/اقتصاص) وقبل الفلتر والتعديلات. */
    private var baseBitmap: Bitmap? = null

    /** النتيجة النهائية المعروضة (فلتر + تعديلات مطبّقة على baseBitmap). */
    private var workingBitmap: Bitmap? = null

    private var currentFilter: FilterType = FilterType.ORIGINAL
    private var filterAdapter: FilterAdapter? = null

    // قيم التعديل (السطوع/التباين/التشبع 0..2، الدفء والحدة -1..1)
    private var brightness = 1.0f
    private var contrast = 1.0f
    private var saturation = 1.0f
    private var warmth = 0.0f
    private var sharpness = 0.0f

    // الطبقات (نصوص/ملصقات): نموذج البيانات + العناصر المرئية الموافقة
    private val overlays = mutableListOf<OverlayItem>()
    private val overlayViews = mutableListOf<View>()
    private var selectedView: View? = null

    private var currentTextColor = Color.WHITE
    private var currentTypeface: Typeface = Typeface.DEFAULT

    private val history = EditorHistory(maxSize = 10)
    private var renderJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupBottomNav()
        setupImage()
        setupAdjustSliders()
        setupTextTab()
        setupStickers()
        setupCropButtons()
    }

    // ── الإعداد الأولي ─────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.toolbarEditor.setNavigationOnClickListener { finish() }
        binding.toolbarEditor.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> { saveImage(); true }
                R.id.action_share -> { shareImage(); true }
                R.id.action_undo -> { undo(); true }
                R.id.action_redo -> { redo(); true }
                else -> false
            }
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_filters -> showTab(Tab.FILTERS)
                R.id.nav_adjust -> showTab(Tab.ADJUST)
                R.id.nav_text -> showTab(Tab.TEXT)
                R.id.nav_stickers -> showTab(Tab.STICKERS)
                R.id.nav_crop -> showTab(Tab.CROP)
            }
            true
        }
    }

    private fun setupImage() {
        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI) ?: run {
            failNoImage()
            return
        }
        gpuImage = GPUImage(this)
        try {
            val bitmap = decodeSampledBitmap(Uri.parse(uriString), MAX_IMAGE_DIMENSION) ?: run {
                failNoImage()
                return
            }
            sourceBitmap = bitmap
            baseBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            workingBitmap = baseBitmap
            binding.ivPreview.setImageBitmap(workingBitmap)
            gpuImage.setImage(baseBitmap!!)
            setupFilters()
        } catch (e: Exception) {
            failNoImage()
        }
    }

    private fun failNoImage() {
        Snackbar.make(binding.root, R.string.msg_no_image_selected, Snackbar.LENGTH_SHORT).show()
        finish()
    }

    private fun showTab(tab: Tab) {
        binding.rvFilters.visibility = if (tab == Tab.FILTERS) View.VISIBLE else View.GONE
        binding.layoutAdjust.visibility = if (tab == Tab.ADJUST) View.VISIBLE else View.GONE
        binding.layoutText.visibility = if (tab == Tab.TEXT) View.VISIBLE else View.GONE
        binding.rvStickers.visibility = if (tab == Tab.STICKERS) View.VISIBLE else View.GONE
        binding.layoutCrop.visibility = if (tab == Tab.CROP) View.VISIBLE else View.GONE
        binding.cropOverlay.visibility = if (tab == Tab.CROP) View.VISIBLE else View.GONE
        if (tab == Tab.CROP) binding.cropOverlay.setAspectRatio(currentAspectRatio)
    }

    // ── الفلاتر ────────────────────────────────────────────────────────

    private fun setupFilters() {
        val filters = FilterType.values().toList()
        val src = sourceBitmap ?: return
        val thumbnails = filters.map { it.generateThumbnail(src, 96) }
        val adapter = FilterAdapter(filters, thumbnails) { applyFilter(it) }
        filterAdapter = adapter
        binding.rvFilters.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFilters.adapter = adapter
    }

    private fun applyFilter(type: FilterType) {
        if (type == currentFilter) return
        pushUndo()
        currentFilter = type
        renderNow()
    }

    // ── التعديلات ─────────────────────────────────────────────────────

    private fun setupAdjustSliders() {
        binding.seekBrightness.bind { brightness = it; scheduleRender() }
        binding.seekContrast.bind { contrast = it; scheduleRender() }
        binding.seekSaturation.bind { saturation = it; scheduleRender() }
        binding.seekWarmth.bind { warmth = it - 1f; scheduleRender() }
        binding.seekSharpness.bind { sharpness = it - 1f; scheduleRender() }
    }

    private fun SeekBar.bind(onChanged: (Float) -> Unit) {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onChanged(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                pushUndo()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // ── خط العرض (فلتر + تعديلات في مجموعة واحدة للحفاظ على الجودة) ──

    private fun buildFilterList(): MutableList<GPUImageFilter> {
        val list = mutableListOf<GPUImageFilter>()
        if (currentFilter != FilterType.ORIGINAL) list += currentFilter.createGpuFilter()
        if (brightness != 1.0f) list += GPUImageBrightnessFilter(brightness - 1f)
        if (contrast != 1.0f) list += GPUImageContrastFilter(contrast)
        if (saturation != 1.0f) list += GPUImageSaturationFilter(saturation)
        if (warmth != 0f) list += GPUImageRGBFilter(1f + warmth * 0.2f, 1f, 1f - warmth * 0.2f)
        if (sharpness != 0f) list += GPUImageSharpenFilter(sharpness)
        return list
    }

    private fun renderImage(): Bitmap {
        val base = baseBitmap ?: return sourceBitmap!!
        val filters = buildFilterList()
        if (filters.isEmpty()) return base
        gpuImage.setImage(base)
        gpuImage.setFilter(GPUImageFilterGroup(filters))
        return gpuImage.bitmapWithFilterApplied
    }

    private fun renderNow() {
        renderJob?.cancel()
        val result = renderImage()
        setWorking(result)
    }

    private fun scheduleRender() {
        renderJob?.cancel()
        renderJob = lifecycleScope.launch {
            delay(40)
            renderNow()
        }
    }

    private fun setWorking(new: Bitmap) {
        val old = workingBitmap
        if (old != null && old !== new && old !== sourceBitmap && old !== baseBitmap && !old.isRecycled) {
            old.recycle()
        }
        workingBitmap = new
        binding.ivPreview.setImageBitmap(new)
    }

    private fun setBase(new: Bitmap) {
        val old = baseBitmap
        if (old != null && old !== new && old !== sourceBitmap && old !== workingBitmap && !old.isRecycled) {
            old.recycle()
        }
        baseBitmap = new
    }

    // ── النص ───────────────────────────────────────────────────────────

    private fun setupTextTab() {
        binding.btnAddText.setOnClickListener {
            val text = binding.etTextInput.text.toString().trim()
            if (text.isNotEmpty()) {
                addTextOverlay(text)
                binding.etTextInput.text.clear()
            }
        }
        binding.btnTextColor.setOnClickListener {
            AmbilWarnaDialog(this, currentTextColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog) {}
                override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                    currentTextColor = color
                    applyTextStyleToSelected()
                }
            }).show()
        }
        setupFontOptions()
    }

    private fun setupFontOptions() {
        val fonts = listOf(
            "sans-serif" to "عادي",
            "sans-serif-medium" to "متوسط",
            "sans-serif-condensed" to "مكثّف",
            "sans-serif-light" to "رفيع",
            "serif" to "سيريف",
            "monospace" to "أحادي",
            "cursive" to "يدوي"
        )
        for ((name, label) in fonts) {
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isCheckedIconVisible = false
                setOnClickListener {
                    currentTypeface = Typeface.create(name, Typeface.NORMAL)
                    applyTextStyleToSelected()
                }
            }
            binding.fontOptions.addView(chip)
        }
    }

    private fun addTextOverlay(text: String) {
        pushUndo()
        val sizePx = 32f * resources.displayMetrics.scaledDensity
        val item = OverlayItem.Text(
            text = text,
            color = currentTextColor,
            typeface = currentTypeface,
            centerX = 0.5f,
            centerY = 0.5f,
            sizePx = sizePx
        )
        overlays.add(item)
        addOverlayView(item)
    }

    private fun applyTextStyleToSelected() {
        val view = selectedView ?: return
        val idx = overlayViews.indexOf(view)
        val item = overlays.getOrNull(idx) as? OverlayItem.Text ?: return
        overlays[idx] = item.copy(color = currentTextColor, typeface = currentTypeface)
        (view as TextView).apply {
            setTextColor(currentTextColor)
            typeface = currentTypeface
        }
    }

    // ── الملصقات ──────────────────────────────────────────────────────

    private fun setupStickers() {
        val stickers = listOf(
            "😀", "😍", "🥰", "😎", "🤩", "😂", "🥳", "😇",
            "🌟", "⭐", "✨", "💫", "🔥", "💯", "🎉", "🎊",
            "❤️", "💜", "💙", "💚", "🧡", "💛", "🤍", "🖤",
            "🌸", "🌺", "🌻", "🌷", "🌹", "🍀", "🌈", "☀️",
            "📷", "🎨", "🖌️", "✏️", "📝", "💡", "🎯", "🏆"
        )
        val adapter = StickerAdapter(stickers) { emoji -> addStickerOverlay(emoji) }
        binding.rvStickers.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvStickers.adapter = adapter
    }

    private fun addStickerOverlay(emoji: String) {
        pushUndo()
        val sizePx = 48f * resources.displayMetrics.scaledDensity
        val item = OverlayItem.Sticker(emoji, 0.5f, 0.5f, sizePx)
        overlays.add(item)
        addOverlayView(item)
    }

    // ── إدارة الطبقات المرئية ─────────────────────────────────────────

    private fun addOverlayView(item: OverlayItem) {
        val container = when (item) {
            is OverlayItem.Text -> binding.textOverlay
            is OverlayItem.Sticker -> binding.stickerOverlay
        }
        val view = TextView(this).apply {
            when (item) {
                is OverlayItem.Text -> {
                    text = item.text
                    setTextColor(item.color)
                    typeface = item.typeface
                    setShadowLayer(8f, 2f, 2f, Color.BLACK)
                    setPadding(16, 8, 16, 8)
                }
                is OverlayItem.Sticker -> {
                    text = item.emoji
                }
            }
            setTextSize(TypedValue.COMPLEX_UNIT_PX, item.sizePx)
        }
        container.addView(view)
        view.post {
            val cw = container.width
            val ch = container.height
            if (cw > 0 && ch > 0) {
                view.x = item.centerX * cw - view.width / 2f
                view.y = item.centerY * ch - view.height / 2f
            }
        }
        val controller = OverlayDragController(container).apply {
            onTransform = { v, cx, cy, size ->
                val idx = overlayViews.indexOf(v)
                if (idx >= 0) overlays[idx] = updateOverlay(overlays[idx], cx, cy, size)
            }
            onSelect = { v -> selectOverlay(v) }
        }
        controller.attach(
            view,
            sizeProvider = { view.textSize },
            sizeSetter = { view.setTextSize(TypedValue.COMPLEX_UNIT_PX, it) }
        )
        view.setOnLongClickListener { deleteOverlay(view); true }
        overlayViews.add(view)
        selectOverlay(view)
    }

    private fun updateOverlay(item: OverlayItem, cx: Float, cy: Float, size: Float): OverlayItem =
        when (item) {
            is OverlayItem.Text -> item.copy(centerX = cx, centerY = cy, sizePx = size)
            is OverlayItem.Sticker -> item.copy(centerX = cx, centerY = cy, sizePx = size)
        }

    private fun selectOverlay(view: View) {
        deselectOverlay()
        selectedView = view
        view.setBackgroundResource(R.drawable.bg_overlay_selected)
    }

    private fun deselectOverlay() {
        selectedView?.setBackgroundResource(0)
        selectedView = null
    }

    private fun deleteOverlay(view: View) {
        val idx = overlayViews.indexOf(view)
        if (idx < 0) return
        pushUndo()
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        overlayViews.removeAt(idx)
        overlays.removeAt(idx)
        if (selectedView === view) selectedView = null
    }

    private fun rebuildOverlayViews() {
        binding.textOverlay.removeAllViews()
        binding.stickerOverlay.removeAllViews()
        overlayViews.clear()
        for (item in overlays) addOverlayView(item)
    }

    // ── التدوير / القلب / الاقتصاص ────────────────────────────────────

    private fun setupCropButtons() {
        binding.btnRotateLeft.setOnClickListener { rotate(-90) }
        binding.btnRotateRight.setOnClickListener { rotate(90) }
        binding.btnFlipH.setOnClickListener { flip(horizontal = true) }
        binding.btnFlipV.setOnClickListener { flip(horizontal = false) }
        binding.btnApplyCrop.setOnClickListener { applyCrop() }

        binding.chipGroupAspect.setOnCheckedStateChangeListener { group, checkedIds ->
            currentAspectRatio = when (checkedIds.firstOrNull()) {
                R.id.chip_square -> 1f
                R.id.chip_4_3 -> 4f / 3f
                R.id.chip_3_4 -> 3f / 4f
                R.id.chip_16_9 -> 16f / 9f
                R.id.chip_9_16 -> 9f / 16f
                else -> null
            }
            binding.cropOverlay.setAspectRatio(currentAspectRatio)
        }
    }

    private var currentAspectRatio: Float? = null

    private fun rotate(degrees: Int) {
        val base = baseBitmap ?: return
        pushUndo()
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
        setBase(rotated)
        renderNow()
    }

    private fun flip(horizontal: Boolean) {
        val base = baseBitmap ?: return
        pushUndo()
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        val flipped = Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
        setBase(flipped)
        renderNow()
    }

    private fun applyCrop() {
        val base = baseBitmap ?: return
        val overlay = binding.cropOverlay
        val rect = overlay.getCropRect()
        if (rect.isEmpty || overlay.width <= 0 || overlay.height <= 0) return

        val content = fitCenterRect(base.width, base.height, overlay.width, overlay.height)
        val left = rect.left.coerceIn(content.left, content.right)
        val top = rect.top.coerceIn(content.top, content.bottom)
        val right = rect.right.coerceIn(content.left, content.right)
        val bottom = rect.bottom.coerceIn(content.top, content.bottom)
        if (right - left < 2f || bottom - top < 2f) return

        val scale = base.width / content.width()
        val bx = ((left - content.left) * scale).toInt().coerceIn(0, base.width - 1)
        val by = ((top - content.top) * scale).toInt().coerceIn(0, base.height - 1)
        val bw = ((right - left) * scale).toInt().coerceIn(1, base.width - bx)
        val bh = ((bottom - top) * scale).toInt().coerceIn(1, base.height - by)

        pushUndo()
        val cropped = Bitmap.createBitmap(base, bx, by, bw, bh)
        setBase(cropped)
        renderNow()
    }

    private fun fitCenterRect(bw: Int, bh: Int, vw: Int, vh: Int): RectF {
        val s = min(vw / bw.toFloat(), vh / bh.toFloat())
        val w = bw * s
        val h = bh * s
        val l = (vw - w) / 2f
        val t = (vh - h) / 2f
        return RectF(l, t, l + w, t + h)
    }

    // ── التراجع / الإعادة ─────────────────────────────────────────────

    private fun captureSnapshot(): EditorHistory.Snapshot {
        val src = baseBitmap ?: sourceBitmap!!
        return EditorHistory.Snapshot(
            bitmap = src.copy(Bitmap.Config.ARGB_8888, true),
            overlays = overlays.toList(),
            filter = currentFilter,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth,
            sharpness = sharpness
        )
    }

    private fun pushUndo() {
        history.push(captureSnapshot())
    }

    private fun undo() {
        val snap = history.undo(captureSnapshot()) ?: run {
            Toast.makeText(this, R.string.action_undo, Toast.LENGTH_SHORT).show()
            return
        }
        applySnapshot(snap)
    }

    private fun redo() {
        val snap = history.redo(captureSnapshot()) ?: run {
            Toast.makeText(this, R.string.action_redo, Toast.LENGTH_SHORT).show()
            return
        }
        applySnapshot(snap)
    }

    private fun applySnapshot(snap: EditorHistory.Snapshot) {
        setBase(snap.bitmap)
        currentFilter = snap.filter
        brightness = snap.brightness
        contrast = snap.contrast
        saturation = snap.saturation
        warmth = snap.warmth
        sharpness = snap.sharpness

        binding.seekBrightness.progress = (brightness * 100).toInt()
        binding.seekContrast.progress = (contrast * 100).toInt()
        binding.seekSaturation.progress = (saturation * 100).toInt()
        binding.seekWarmth.progress = ((warmth + 1f) * 100).toInt()
        binding.seekSharpness.progress = ((sharpness + 1f) * 100).toInt()

        overlays.clear()
        overlays.addAll(snap.overlays)
        rebuildOverlayViews()

        filterAdapter?.setSelected(FilterType.values().indexOf(currentFilter))
        renderNow()
    }

    // ── الحفظ / المشاركة ──────────────────────────────────────────────

    private fun saveImage() {
        val bmp = workingBitmap ?: return
        val overlaysSnapshot = overlays.toList()
        val cw = binding.previewContainer.width
        val ch = binding.previewContainer.height
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val exported = ImageExporter.render(bmp, overlaysSnapshot, cw, ch)
                ImageSaver.save(this@EditorActivity, exported)
                if (exported !== bmp) exported.recycle()
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    Snackbar.make(binding.root, R.string.msg_image_saved, Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    Snackbar.make(binding.root, R.string.msg_save_failed, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareImage() {
        val bmp = workingBitmap ?: return
        val overlaysSnapshot = overlays.toList()
        val cw = binding.previewContainer.width
        val ch = binding.previewContainer.height
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val exported = ImageExporter.render(bmp, overlaysSnapshot, cw, ch)
                val uri = ImageSaver.save(this@EditorActivity, exported)
                if (exported !== bmp) exported.recycle()
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    Snackbar.make(binding.root, R.string.msg_save_failed, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── تحميل الصورة مع تصغير آمن للذاكرة ─────────────────────────────

    private fun decodeSampledBitmap(uri: Uri, reqSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w / 2 >= reqSize || h / 2 >= reqSize) {
            w /= 2
            h /= 2
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    // ── التنظيف ───────────────────────────────────────────────────────

    override fun onDestroy() {
        renderJob?.cancel()
        history.clearAll()
        val src = sourceBitmap
        val base = baseBitmap
        val work = workingBitmap
        recycle(work?.takeIf { it !== src && it !== base })
        recycle(base?.takeIf { it !== src })
        recycle(src)
        super.onDestroy()
    }

    private fun recycle(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
    }

    enum class Tab { FILTERS, ADJUST, TEXT, STICKERS, CROP }
}
