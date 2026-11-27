package io.github.aiyzkn.lyra.data

import android.Manifest
import android.os.Build

/** 曲を読むのに要る権限。Android 13 から `READ_MEDIA_AUDIO` に分かれた。 */
fun audioPermission(sdkInt: Int = Build.VERSION.SDK_INT): String =
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
