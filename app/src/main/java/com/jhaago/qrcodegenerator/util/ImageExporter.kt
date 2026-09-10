package com.jhaago.qrcodegenerator.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object ImageExporter {
    fun saveToGallery(context: Context, bitmap: Bitmap): Uri {
        val resolver = context.contentResolver
        val fileName = "qr-code-${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/QR Code Generator",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = requireNotNull(
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
        ) { "Unable to create image in MediaStore." }

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    "Unable to encode PNG image."
                }
            } ?: error("Unable to open image output stream.")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    fun createShareIntent(context: Context, bitmap: Bitmap): Intent {
        val shareDirectory = File(context.cacheDir, "shared_qr").apply { mkdirs() }
        val imageFile = File(shareDirectory, "qr-code-${System.currentTimeMillis()}.png")
        imageFile.outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Unable to encode PNG image."
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share QR code")
    }
}
