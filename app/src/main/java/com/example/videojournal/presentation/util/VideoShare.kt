package com.example.videojournal.presentation.util

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
private const val VIDEO_MIME_TYPE = "video/mp4"

fun Activity.shareVideoFile(
    file: File,
    chooserTitle: String,
) {
    val uri = FileProvider.getUriForFile(
        this,
        "$packageName$FILE_PROVIDER_AUTHORITY_SUFFIX",
        file,
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = VIDEO_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    startActivity(Intent.createChooser(shareIntent, chooserTitle))
}
