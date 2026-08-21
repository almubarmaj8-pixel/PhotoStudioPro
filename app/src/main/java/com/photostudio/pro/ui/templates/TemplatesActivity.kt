package com.photostudio.pro.ui.templates

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.photostudio.pro.R
import com.photostudio.pro.databinding.ActivityTemplatesBinding
import com.photostudio.pro.ui.editor.EditorActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TemplatesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplatesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTemplatesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupTemplates()
        binding.toolbarTemplates.setNavigationOnClickListener { finish() }
    }

    private fun setupTemplates() {
        val templates = listOf(
            Template("Instagram Post", "1080 × 1080", "1:1", 1080, 1080),
            Template("Instagram Story", "1080 × 1920", "9:16", 1080, 1920),
            Template("Facebook Cover", "820 × 312", "2.6:1", 820, 312),
            Template("YouTube Thumbnail", "1280 × 720", "16:9", 1280, 720),
            Template("WhatsApp Status", "1080 × 1920", "9:16", 1080, 1920),
            Template("Poster", "2480 × 3508", "A4", 2480, 3508),
            Template("Collage 2 photos", "1080 × 1080", "1:1", 1080, 1080, isCollage = true),
            Template("Collage 4 photos", "1080 × 1080", "1:1", 1080, 1080, isCollage = true),
        )
        val adapter = TemplateAdapter(templates) { template -> onTemplateClicked(template) }
        binding.rvTemplates.layoutManager = GridLayoutManager(this, 2)
        binding.rvTemplates.adapter = adapter
    }

    private fun onTemplateClicked(template: Template) {
        if (template.isCollage) {
            Snackbar.make(binding.root, R.string.msg_coming_soon, Snackbar.LENGTH_SHORT).show()
            return
        }
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val uri = createCanvasUri(template.width, template.height)
            withContext(Dispatchers.Main) {
                binding.progress.visibility = View.GONE
                if (uri != null) {
                    val intent = Intent(this@TemplatesActivity, EditorActivity::class.java)
                    intent.putExtra(EditorActivity.EXTRA_IMAGE_URI, uri.toString())
                    startActivity(intent)
                } else {
                    Snackbar.make(binding.root, R.string.msg_save_failed, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createCanvasUri(width: Int, height: Int): android.net.Uri? {
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    Color.parseColor("#6C5CE7"), Color.parseColor("#FD79A8"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            val dir = File(cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "template_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }
}
