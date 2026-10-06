package com.sultonuzdev.netspeed.utils

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Play's in-app rating sheet.
 *
 * Play decides whether the sheet actually appears: it enforces its own quota per user, returns
 * nothing for an install that did not come from Play, and reports neither the outcome nor whether
 * anything was shown. So there is no success to react to and no failure worth surfacing -- a
 * request that goes nowhere has to look exactly like one that worked, which is why every error
 * here is swallowed rather than shown.
 *
 * Asking is also the caller's decision, not this file's: see the trigger in SpeedViewModel.
 */
object InAppReview {

    suspend fun show(activity: Activity) {
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            manager.launchReview(activity, manager.requestReview())
        }
    }
}
