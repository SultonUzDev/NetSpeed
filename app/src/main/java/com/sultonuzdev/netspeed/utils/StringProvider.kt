package com.sultonuzdev.netspeed.utils

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Resolves string resources outside of composition.
 *
 * View models and the data layer build text too -- picker labels, notification bodies, the
 * synthetic names for traffic that belongs to no installed package -- and none of them can call
 * [androidx.compose.ui.res.stringResource]. Handing them the Application context directly would
 * work, but it puts an Android dependency in classes that otherwise have none and makes them
 * awkward to test; this is the one seam that needs it.
 *
 * Deliberately thin: it resolves resources and nothing else. Anything that formats a number or a
 * date belongs in the locale-aware formatters, not here.
 */
interface StringProvider {
    fun get(@StringRes id: Int): String
    fun get(@StringRes id: Int, vararg args: Any): String
    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String
}

class AndroidStringProvider(private val context: Context) : StringProvider {

    override fun get(@StringRes id: Int): String = context.getString(id)

    override fun get(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    /**
     * [count] is passed twice on purpose: once to pick the plural form and again as the first
     * format argument, because every one of our plural strings prints the number it counts.
     */
    override fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        context.resources.getQuantityString(
            id,
            count,
            *(if (args.isEmpty()) arrayOf<Any>(count) else args)
        )
}
