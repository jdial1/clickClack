package com.ampersand.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

/** Longest side of a kept photo, in pixels. Keeps the JPEG well under a Firestore document's limit. */
private const val PhotoEdge = 900

/** Read [uri], shrink it to [PhotoEdge] and encode as a base64 JPEG, or null if it can't be read. */
fun compressPhoto(context: Context, uri: Uri): String? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PhotoEdge) sample *= 2
    val raw = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val scale = PhotoEdge.toFloat() / maxOf(raw.width, raw.height)
    val sized = if (scale < 1f) Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt(), (raw.height * scale).toInt(), true) else raw
    var quality = 80
    var bytes: ByteArray
    do {
        bytes = ByteArrayOutputStream().also { sized.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
        quality -= 15
    } while (bytes.size > 600_000 && quality > 20)
    Base64.encodeToString(bytes, Base64.NO_WRAP)
}.getOrNull()

/** Decode a kept photo once per composition. */
@Composable
fun rememberPhoto(data: String): ImageBitmap? = remember(data) {
    runCatching {
        val bytes = Base64.decode(data, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}
