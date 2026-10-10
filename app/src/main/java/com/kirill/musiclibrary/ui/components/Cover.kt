package com.kirill.musiclibrary.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kirill.musiclibrary.domain.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Small in-memory bitmap cache keyed by asset path + decode size. */
private object CoverCache {
    private val cache = LruCache<String, ImageBitmap>(24)
    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, value: ImageBitmap) {
        cache.put(key, value)
    }
}

private fun loadAssetBitmap(context: Context, path: String, targetPx: Int): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (longest / sample > targetPx * 2) sample *= 2
    val decode = BitmapFactory.Options().apply { inSampleSize = sample }
    context.assets.open(path).use { stream ->
        BitmapFactory.decodeStream(stream, null, decode)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}

/**
 * Displays an album cover from a packaged asset, falling back to the deterministic gradient +
 * initials placeholder when no art exists (or the asset fails to load). The loader replaces
 * Coil for this offline, asset-only use case: no network and no extra dependency.
 */
@Composable
fun AssetCover(
    coverAsset: String?,
    title: String,
    modifier: Modifier = Modifier,
    targetPx: Int = 512,
    contentDescription: String? = null,
) {
    if (coverAsset.isNullOrBlank()) {
        CoverPlaceholder(title, modifier, contentDescription)
        return
    }
    val context = LocalContext.current
    val key = "$coverAsset@$targetPx"
    var bitmap by remember(key) { mutableStateOf(CoverCache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) {
                loadAssetBitmap(context, "library/$coverAsset", targetPx)?.also { CoverCache.put(key, it) }
            }
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        CoverPlaceholder(title, modifier, contentDescription)
    }
}

/** Deterministic gradient + initials, matching LIBRARY.html `hue()`/`initials()`. */
@Composable
fun CoverPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val hue = Format.hue(title)
    val top = Color.hsv(hue.toFloat(), 0.55f, 0.72f)
    val bottom = Color.hsv(((hue + 70) % 360).toFloat(), 0.55f, 0.52f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Format.initials(title),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
