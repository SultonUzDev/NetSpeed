package com.sultonuzdev.netspeed.utils

import androidx.annotation.StringRes
import com.sultonuzdev.netspeed.R

/** Which speeds the notification and its status-bar icon show. */
enum class SpeedDisplayMode(val prefName: String, @param:StringRes val labelRes: Int) {
    /** Download only -- one large number. The classic speed-indicator look. */
    DOWNLOAD("download", R.string.display_download_only),

    /** Upload only. */
    UPLOAD("upload", R.string.display_upload_only),

    /** Download and upload stacked, arrows included. */
    BOTH("both", R.string.display_download_and_upload),

    /** Their sum as a single figure. */
    COMBINED("combined", R.string.display_combined_total);

    companion object {
        fun fromPrefName(name: String?): SpeedDisplayMode =
            entries.firstOrNull { it.prefName == name?.lowercase() } ?: DOWNLOAD
    }
}
