package com.sultonuzdev.netspeed.presentation.components

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads and remembers app icons.
 *
 * Resolving an icon is a PackageManager binder call plus rasterising a drawable -- adaptive icons
 * are composed from layers, so it is real work. Doing that inline in composition meant every
 * newly scrolled row blocked a frame, and scrolling back decoded the same icon again.
 *
 * Entries are kept for the process lifetime: the set of installed apps is small and bounded, and
 * a null result is cached too so unresolvable packages are not retried on every recomposition.
 */
object AppIconCache {

    private const val ICON_PX = 96

    private val cache = ConcurrentHashMap<String, Optional>()

    /** ConcurrentHashMap cannot store nulls, so absence and "known to be absent" are distinct. */
    private class Optional(val bitmap: ImageBitmap?)

    /** Cached value if it has already been loaded, without touching PackageManager. */
    fun peek(packageName: String): ImageBitmap? = cache[packageName]?.bitmap

    fun isLoaded(packageName: String): Boolean = cache.containsKey(packageName)

    suspend fun load(context: Context, packageName: String): ImageBitmap? {
        cache[packageName]?.let { return it.bitmap }

        val bitmap = withContext(Dispatchers.IO) {
            try {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(ICON_PX, ICON_PX)
                    .asImageBitmap()
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } catch (e: Exception) {
                null
            }
        }

        cache[packageName] = Optional(bitmap)
        return bitmap
    }
}
