package com.photostudio.pro.ui.templates

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.photostudio.pro.databinding.ActivityTemplatesBinding

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
            Template("Instagram Post", "1080 × 1080", "1:1"),
            Template("Instagram Story", "1080 × 1920", "9:16"),
            Template("Facebook Cover", "820 × 312", "2.6:1"),
            Template("YouTube Thumbnail", "1280 × 720", "16:9"),
            Template("WhatsApp Status", "1080 × 1920", "9:16"),
            Template("Poster", "2480 × 3508", "A4"),
            Template("Collage 2 photos", "1080 × 1080", "1:1"),
            Template("Collage 4 photos", "1080 × 1080", "1:1"),
        )
        val adapter = TemplateAdapter(templates)
        binding.rvTemplates.layoutManager = GridLayoutManager(this, 2)
        binding.rvTemplates.adapter = adapter
    }
}
