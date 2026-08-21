package com.photostudio.pro.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.photostudio.pro.R
import com.photostudio.pro.databinding.ActivityMainBinding
import com.photostudio.pro.ui.editor.EditorActivity
import com.photostudio.pro.ui.templates.TemplatesActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var cameraUri: Uri? = null

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) openEditor(uri.toString())
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) cameraUri?.let { openEditor(it.toString()) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        setupRecent()
    }

    private fun setupViews() {
        // GetContent يستخدم منتقي النظام ولا يتطلب إذن تخزين صريحاً
        binding.cardOpenImage.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.cardCamera.setOnClickListener { openCamera() }
        binding.cardTemplates.setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
        }
        binding.cardCollage.setOnClickListener {
            Snackbar.make(binding.root, R.string.msg_coming_soon, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupRecent() {
        binding.rvRecent.layoutManager =
            GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false)
        binding.tvNoRecent.visibility = View.VISIBLE
    }

    private fun openCamera() {
        cameraUri = createCameraUri()
        cameraUri?.let { uri -> cameraLauncher.launch(uri) }
            ?: Snackbar.make(binding.root, R.string.msg_save_failed, Snackbar.LENGTH_SHORT).show()
    }

    private fun createCameraUri(): Uri? {
        val dir = File(cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    private fun openEditor(imageUri: String) {
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra(EditorActivity.EXTRA_IMAGE_URI, imageUri)
        startActivity(intent)
    }
}
