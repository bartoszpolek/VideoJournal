package com.example.videojournal.presentation.record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

internal val RecordPermissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
)

internal fun Context.currentRecordPermissions(activity: Activity?): RecordIntent.PermissionsResolved {
    val cameraGranted = hasPermission(Manifest.permission.CAMERA)
    val audioGranted = hasPermission(Manifest.permission.RECORD_AUDIO)

    return RecordIntent.PermissionsResolved(
        cameraGranted = cameraGranted,
        audioGranted = audioGranted,
        permanentlyDenied = activity.hasPermanentlyDeniedAny(
            deniedRecordPermissions(
                cameraGranted = cameraGranted,
                audioGranted = audioGranted,
            ),
        ),
    )
}

internal fun Context.applicationSettingsIntent(): Intent =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun deniedRecordPermissions(
    cameraGranted: Boolean,
    audioGranted: Boolean,
): List<String> = buildList {
    if (!cameraGranted) add(Manifest.permission.CAMERA)
    if (!audioGranted) add(Manifest.permission.RECORD_AUDIO)
}

private fun Activity?.hasPermanentlyDeniedAny(permissions: Collection<String>): Boolean =
    this != null && permissions.any { permission ->
        !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }
