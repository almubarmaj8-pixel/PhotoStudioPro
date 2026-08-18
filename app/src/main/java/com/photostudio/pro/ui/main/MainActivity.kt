package com.photostudio.pro.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.photostudio.pro.R
import com.photostudio.pro.databinding.ActivityMainBinding
import com.photostudio.pro.ui.editor.EditorActivity
import com.photostudio.pro.ui.templates.TemplatesActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                openEditor(uri.toString())
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            // Preview returned - in production, save and open editor
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openGallery()
            else Snackbar.make(binding.root, R.string.msg_permission_required, Snackbar.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        setupRecent()
    }

    private fun setupViews() {
        binding.cardOpenImage.setOnClickListener { checkPermissionAndOpenGallery() }
        binding.cardCamera.setOnClickListener { checkPermissionAndOpenGallery() }
        binding.cardTemplates.setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
        }
        binding.cardCollage.setOnClickListener {
            Snackbar.make(binding.root, "قريباً: تكوين الصور", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupRecent() {
        binding.rvRecent.layoutManager =
            GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false)
        binding.tvNoRecent.visibility = View.VISIBLE
    }

    private fun checkPermissionAndOpenGallery() {
        val perm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            permissionLauncher.launch(perm)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun openEditor(imageUri: String) {
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra(EditorActivity.EXTRA_IMAGE_URI, imageUri)
        startActivity(intent)
    }
}
