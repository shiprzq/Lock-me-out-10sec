package com.agon.app.ui.screens

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRelative(ts: Long): String {
    if (ts == 0L) return "never"
    return DateUtils.getRelativeTimeSpanString(
        ts,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

fun formatAbsolute(ts: Long): String {
    if (ts == 0L) return "—"
    return try {
        SimpleDateFormat("MMM d, yyyy • h:mm:ss a", Locale.getDefault()).format(Date(ts))
    } catch (_: Exception) {
        ts.toString()
    }
}

fun formatClock(ts: Long): String {
    if (ts == 0L) return "—"
    return try {
        SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(ts))
    } catch (_: Exception) {
        ""
    }
}
