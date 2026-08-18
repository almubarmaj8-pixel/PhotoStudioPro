package com.photostudio.pro.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.photostudio.pro.R
import com.photostudio.pro.databinding.ActivityEditorBinding
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }

    private lateinit var binding: ActivityEditorBinding
    private lateinit var gpuImage: GPUImage
    private var originalBitmap: Bitmap? = null
    private var workingBitmap: Bitmap? = null
    private var currentFilter: GPUImageFilter? = null
    private var rotationDegrees = 0
    private var isFlippedH = false
    private var isFlippedV = false

    // Adjust values (0-200, default 100)
    private var brightness = 1.0f
    private var contrast = 1.0f
    private var saturation = 1.0f
    private var warmth = 0.0f
    private var sharpness = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupBottomNav()
        setupImage()
        setupAdjustSliders()
        setupTextTab()
        setupCropButtons()
        setupStickers()
    }

    private fun setupToolbar() {
        binding.toolbarEditor.setNavigationOnClickListener { finish() }
        binding.toolbarEditor.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> { saveImage(); true }
                R.id.action_share -> { shareImage(); true }
                R.id.action_undo -> { Toast.makeText(this, "تراجع", Toast.LENGTH_SHORT).show(); true }
                R.id.action_redo -> { Toast.makeText(this, "إعادة", Toast.LENGTH_SHORT).show(); true }
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
            Snackbar.make(binding.root, R.string.msg_no_image_selected, Snackbar.LENGTH_SHORT).show()
            finish()
            return
        }
        gpuImage = GPUImage(this)
        try {
            val inputStream = contentResolver.openInputStream(Uri.parse(uriString))
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            workingBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            gpuImage.setImage(workingBitmap!!)
            binding.ivPreview.setImageBitmap(workingBitmap)
            setupFilters()
        } catch (e: Exception) {
            Snackbar.make(binding.root, R.string.msg_no_image_selected, Snackbar.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showTab(tab: Tab) {
        binding.rvFilters.visibility = if (tab == Tab.FILTERS) View.VISIBLE else View.GONE
        binding.layoutAdjust.visibility = if (tab == Tab.ADJUST) View.VISIBLE else View.GONE
        binding.layoutText.visibility = if (tab == Tab.TEXT) View.VISIBLE else View.GONE
        binding.rvStickers.visibility = if (tab == Tab.STICKERS) View.VISIBLE else View.GONE
        binding.layoutCrop.visibility = if (tab == Tab.CROP) View.VISIBLE else View.GONE
    }

    // ── FILTERS ──────────────────────────────────────────

    private fun setupFilters() {
        val filters = FilterType.values().toList()
        val adapter = FilterAdapter(filters, workingBitmap!!) { filterType ->
            applyFilter(filterType)
        }
        binding.rvFilters.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFilters.adapter = adapter
    }

    private fun applyFilter(filterType: FilterType) {
        val filter = when (filterType) {
            FilterType.ORIGINAL -> GPUImageFilter()
            FilterType.WARM -> GPUImageBrightnessFilter(0.1f)
            FilterType.COOL -> GPUImageHueFilter(210f)
            FilterType.VINTAGE -> GPUImageSepiaToneFilter(0.7f)
            FilterType.BW -> GPUImageGrayscaleFilter()
            FilterType.VIVID -> GPUImageSaturationFilter(1.8f)
            FilterType.FADE -> GPUImageSaturationFilter(0.5f)
            FilterType.DRAMATIC -> GPUImageContrastFilter(1.5f)
            FilterType.SEPIA -> GPUImageSepiaToneFilter(1.0f)
            FilterType.NOIR -> GPUImageGrayscaleFilter()
            FilterType.SUNSET -> GPUImageRGBFilter(1.2f, 1.0f, 0.7f)
            FilterType.FOREST -> GPUImageHueFilter(90f)
        }
        currentFilter = filter
        gpuImage.setFilter(filter)
        val filtered = gpuImage.bitmapWithFilterApplied
        binding.ivPreview.setImageBitmap(filtered)
        workingBitmap = filtered
    }

    // ── ADJUST ───────────────────────────────────────────

    private fun setupAdjustSliders() {
        binding.seekBrightness.setOnSeekBarChangeListener(object : SeekBarCallback {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                brightness = progress / 100f
                applyAdjustments()
            }
        })
        binding.seekContrast.setOnSeekBarChangeListener(object : SeekBarCallback {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                contrast = progress / 100f
                applyAdjustments()
            }
        })
        binding.seekSaturation.setOnSeekBarChangeListener(object : SeekBarCallback {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                saturation = progress / 100f
                applyAdjustments()
            }
        })
        binding.seekWarmth.setOnSeekBarChangeListener(object : SeekBarCallback {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                warmth = (progress - 100) / 100f
                applyAdjustments()
            }
        })
        binding.seekSharpness.setOnSeekBarChangeListener(object : SeekBarCallback {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sharpness = (progress - 100) / 100f
                applyAdjustments()
            }
        })
    }

    private fun applyAdjustments() {
        originalBitmap ?: return
        val filterGroup = GPUImageFilterGroup(
            listOf(
                GPUImageBrightnessFilter(brightness - 1.0f),
                GPUImageContrastFilter(contrast),
                GPUImageSaturationFilter(saturation),
                GPUImageSharpenFilter(sharpness)
            )
        )
        gpuImage.setImage(originalBitmap!!)
        gpuImage.setFilter(filterGroup)
        val result = gpuImage.bitmapWithFilterApplied
        workingBitmap = result
        binding.ivPreview.setImageBitmap(result)
    }

    // ── TEXT ──────────────────────────────────────────────

    private fun setupTextTab() {
        binding.btnAddText.setOnClickListener {
            val text = binding.etTextInput.text.toString().trim()
            if (text.isNotEmpty()) {
                addTextOverlay(text)
                binding.etTextInput.text.clear()
            }
        }
    }

    private fun addTextOverlay(text: String) {
        val textView = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 32f
            setShadowLayer(8f, 2f, 2f, Color.BLACK)
            x = (binding.previewContainer.width / 3).toFloat()
            y = (binding.previewContainer.height / 2).toFloat()
            setPadding(16, 8, 16, 8)
        }
        binding.textOverlay.addView(textView)
        makeTextDraggable(textView)
    }

    private fun makeTextDraggable(textView: TextView) {
        textView.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    view.x = event.rawX - view.width / 2
                    view.y = event.rawY - view.height / 2
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    true
                }
                else -> false
            }
        }
    }

    // ── STICKERS ─────────────────────────────────────────

    private fun setupStickers() {
        val stickers = listOf(
            "😀", "😍", "🥰", "😎", "🤩", "😂", "🥳", "😇",
            "🌟", "⭐", "✨", "💫", "🔥", "💯", "🎉", "🎊",
            "❤️", "💜", "💙", "💚", "🧡", "💛", "🤍", "🖤",
            "🌸", "🌺", "🌻", "🌷", "🌹", "🍀", "🌈", "☀️",
            "📷", "🎨", "🖌️", "✏️", "📝", "💡", "🎯", "🏆"
        )
        val adapter = StickerAdapter(stickers) { emoji ->
            addStickerOverlay(emoji)
        }
        binding.rvStickers.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvStickers.adapter = adapter
    }

    private fun addStickerOverlay(emoji: String) {
        val textView = TextView(this).apply {
            text = emoji
            textSize = 48f
            x = (binding.previewContainer.width / 3).toFloat()
            y = (binding.previewContainer.height / 3).toFloat()
        }
        binding.stickerOverlay.addView(textView)
        makeTextDraggable(textView)
    }

    // ── CROP / ROTATE / FLIP ──────────────────────────────

    private fun setupCropButtons() {
        binding.btnRotateLeft.setOnClickListener { rotate(-90) }
        binding.btnRotateRight.setOnClickListener { rotate(90) }
        binding.btnFlipH.setOnClickListener { flip(horizontal = true) }
        binding.btnFlipV.setOnClickListener { flip(horizontal = false) }
    }

    private fun rotate(degrees: Int) {
        workingBitmap?.let { bmp ->
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            workingBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            binding.ivPreview.setImageBitmap(workingBitmap)
            gpuImage.setImage(workingBitmap!!)
        }
    }

    private fun flip(horizontal: Boolean) {
        workingBitmap?.let { bmp ->
            val matrix = Matrix().apply {
                if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
            }
            workingBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            binding.ivPreview.setImageBitmap(workingBitmap)
            gpuImage.setImage(workingBitmap!!)
        }
    }

    // ── SAVE / SHARE ──────────────────────────────────────

    private fun saveImage() {
        workingBitmap?.let { bmp ->
            binding.progress.visibility = View.VISIBLE
            Thread {
                try {
                    val combined = captureView()
                    val savedUri = ImageSaver.save(this, combined!!)
                    runOnUiThread {
                        binding.progress.visibility = View.GONE
                        Snackbar.make(binding.root, R.string.msg_image_saved, Snackbar.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        binding.progress.visibility = View.GONE
                        Snackbar.make(binding.root, R.string.msg_save_failed, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun shareImage() {
        workingBitmap?.let { bmp ->
            binding.progress.visibility = View.VISIBLE
            Thread {
                try {
                    val combined = captureView()
                    val uri = ImageSaver.save(this, combined!!)
                    runOnUiThread {
                        binding.progress.visibility = View.GONE
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)))
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        binding.progress.visibility = View.GONE
                        Snackbar.make(binding.root, R.string.msg_save_failed, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun captureView(): Bitmap? {
        val container = binding.previewContainer
        val bitmap = Bitmap.createBitmap(container.width, container.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        container.draw(canvas)
        return bitmap
    }

    // ── ENUMS ────────────────────────────────────────────

    enum class Tab { FILTERS, ADJUST, TEXT, STICKERS, CROP }

    // ── SEEKBAR CALLBACK ──────────────────────────────────

    private interface SeekBarCallback : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }
}
