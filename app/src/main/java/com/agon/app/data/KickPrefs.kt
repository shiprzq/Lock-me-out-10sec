package com.agon.app.data

import android.content.Context
import android.content.SharedPreferences

object KickPrefs {
    private const val FILE = "kick_out_prefs"
    private const val KEY_LAST_KICK = "last_kick_time"
    private const val KEY_PENDING = "pending_congrats"
    private const val KEY_TOTAL_KICKS = "total_kicks"
    private const val KEY_HISTORY = "kick_history"
    private const val KEY_FIRST_KICK = "first_kick_time"

    data class Snapshot(
        val lastKickTime: Long = 0L,
        val pendingCongrats: Boolean = false,
        val totalKicks: Int = 0,
        val history: List<Long> = emptyList(),
        val firstKickTime: Long = 0L
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun parseHistory(raw: String?): List<Long> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.sortedDescending()
    }

    private fun serializeHistory(list: List<Long>): String =
        list.sortedDescending().take(50).joinToString(",")

    fun load(context: Context): Snapshot {
        val p = prefs(context)
        return Snapshot(
            lastKickTime = p.getLong(KEY_LAST_KICK, 0L),
            pendingCongrats = p.getBoolean(KEY_PENDING, false),
            totalKicks = p.getInt(KEY_TOTAL_KICKS, 0),
            history = parseHistory(p.getString(KEY_HISTORY, "")),
            firstKickTime = p.getLong(KEY_FIRST_KICK, 0L)
        )
    }

    /** Synchronous commit — MUST be called before kicking the user out so the lock survives instant kill. */
    fun saveKickSync(context: Context, now: Long, totalKicks: Int, prevHistory: List<Long>, firstKick: Long) {
        val newHistory = (listOf(now) + prevHistory).sortedDescending().take(50)
        val resolvedFirst = if (firstKick == 0L) now else firstKick
        prefs(context).edit()
            .putLong(KEY_LAST_KICK, now)
            .putBoolean(KEY_PENDING, true)
            .putInt(KEY_TOTAL_KICKS, totalKicks)
            .putString(KEY_HISTORY, serializeHistory(newHistory))
            .putLong(KEY_FIRST_KICK, resolvedFirst)
            .commit()
    }

    fun clearPendingSync(context: Context) {
        prefs(context).edit().putBoolean(KEY_PENDING, false).commit()
    }

    fun resetSync(context: Context) {
        prefs(context).edit()
            .putLong(KEY_LAST_KICK, 0L)
            .putBoolean(KEY_PENDING, false)
            .putInt(KEY_TOTAL_KICKS, 0)
            .putString(KEY_HISTORY, "")
            .putLong(KEY_FIRST_KICK, 0L)
            .commit()
    }
}
