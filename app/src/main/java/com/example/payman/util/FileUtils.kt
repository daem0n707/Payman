package com.example.payman.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.*

fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri {
    val filename = "bill_${UUID.randomUUID()}.jpg"
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { 
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}
