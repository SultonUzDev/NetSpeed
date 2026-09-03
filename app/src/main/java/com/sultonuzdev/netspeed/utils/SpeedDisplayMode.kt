package com.sultonuzdev.netspeed.utils

/** Which speeds the notification and its status-bar icon show. */
enum class SpeedDisplayMode(val prefName: String, val label: String) {
    /** Download only -- one large number. The classic speed-indicator look. */
    DOWNLOAD("download", "Download only"),

    /** Upload only. */
    UPLOAD("upload", "Upload only"),

    /** Download and upload stacked, arrows included. */
    BOTH("both", "Download and upload"),

    /** Their sum as a single figure. */
    COMBINED("combined", "Combined total");

    companion object {
        fun fromPrefName(name: String?): SpeedDisplayMode =
            entries.firstOrNull { it.prefName == name?.lowercase() } ?: DOWNLOAD
    }
}
