package com.photostudio.pro.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageSaver {

    fun save(context: Context, bitmap: Bitmap): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "PhotoStudio_$timestamp.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, bitmap, filename)
        } else {
            saveLegacy(bitmap, filename)
        }
    }

    private fun saveViaMediaStore(context: Context, bitmap: Bitmap, filename: String): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoStudioPro")
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ) ?: throw Exception("Failed to create MediaStore entry")
        val outputStream: OutputStream = context.contentResolver.openOutputStream(uri)
            ?: throw Exception("Failed to open output stream")
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        outputStream.close()
        return uri
    }

    private fun saveLegacy(bitmap: Bitmap, filename: String): Uri {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "PhotoStudioPro"
        )
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, filename)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        return Uri.fromFile(file)
    }
}
