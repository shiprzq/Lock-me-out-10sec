package com.agon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.data.KickPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KickViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        const val LOCKOUT_SECONDS = 10
        const val LOCKOUT_MILLIS = 10_000L
    }

    sealed interface KickUiState {
        data object Loading : KickUiState
        data class Ready(
            val totalKicks: Int,
            val history: List<Long>,
            val firstKickTime: Long,
            val lastKickTime: Long
        ) : KickUiState
        data class Locked(
            val secondsLeft: Int,
            val progress: Float,
            val totalKicks: Int,
            val kickNumber: Int
        ) : KickUiState
        data class Congrats(
            val kickNumber: Int,
            val totalKicks: Int,
            val history: List<Long>
        ) : KickUiState
    }

    private val _uiState = MutableStateFlow<KickUiState>(KickUiState.Loading)
    val uiState: StateFlow<KickUiState> = _uiState.asStateFlow()

    private var lastKickTime: Long = 0L
    private var pendingCongrats: Boolean = false
    private var totalKicks: Int = 0
    private var history: List<Long> = emptyList()
    private var firstKickTime: Long = 0L
    private var loaded = false
    private var tickerJob: Job? = null

    val taunts = listOf(
        "Nope. Go touch grass.",
        "The button misses you already.",
        "Shhh... exile in progress.",
        "Don't even think about it.",
        "Counting... slowly... on purpose.",
        "You did this to yourself.",
        "Enjoy your freedom. Briefly.",
        "Pretending you don't exist...",
        "Almost there. Probably.",
        "Stay OUT!"
    )

    init {
        val snap = KickPrefs.load(getApplication())
        lastKickTime = snap.lastKickTime
        pendingCongrats = snap.pendingCongrats
        totalKicks = snap.totalKicks
        history = snap.history
        firstKickTime = snap.firstKickTime
        loaded = true
        refresh(System.currentTimeMillis())
    }

    fun refresh(now: Long = System.currentTimeMillis()) {
        if (!loaded) return
        // Re-read from disk in case another process wrote (e.g. after relaunch)
        val snap = KickPrefs.load(getApplication())
        lastKickTime = snap.lastKickTime
        pendingCongrats = snap.pendingCongrats
        totalKicks = snap.totalKicks
        history = snap.history
        firstKickTime = snap.firstKickTime

        if (lastKickTime == 0L) {
            tickerJob?.cancel()
            _uiState.value = KickUiState.Ready(totalKicks, history, firstKickTime, lastKickTime)
            return
        }
        val elapsed = now - lastKickTime
        if (elapsed < 0) {
            enterLocked(LOCKOUT_SECONDS, 0f)
            startTicker()
            return
        }
        if (elapsed < LOCKOUT_MILLIS) {
            val secondsLeft = ((LOCKOUT_MILLIS - elapsed + 999L) / 1000L).toInt().coerceIn(1, LOCKOUT_SECONDS)
            val progress = (elapsed.toFloat() / LOCKOUT_MILLIS.toFloat()).coerceIn(0f, 1f)
            enterLocked(secondsLeft, progress)
            startTicker()
        } else if (pendingCongrats) {
            tickerJob?.cancel()
            _uiState.value = KickUiState.Congrats(totalKicks.coerceAtLeast(1), totalKicks, history)
        } else {
            tickerJob?.cancel()
            _uiState.value = KickUiState.Ready(totalKicks, history, firstKickTime, lastKickTime)
        }
    }

    private fun enterLocked(secondsLeft: Int, progress: Float) {
        _uiState.value = KickUiState.Locked(
            secondsLeft = secondsLeft,
            progress = progress,
            totalKicks = totalKicks,
            kickNumber = totalKicks.coerceAtLeast(1)
        )
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(200L)
                val now = System.currentTimeMillis()
                val elapsed = now - lastKickTime
                if (elapsed < 0) {
                    enterLocked(LOCKOUT_SECONDS, 0f)
                } else if (elapsed < LOCKOUT_MILLIS) {
                    val secondsLeft = ((LOCKOUT_MILLIS - elapsed + 999L) / 1000L).toInt().coerceIn(1, LOCKOUT_SECONDS)
                    val progress = (elapsed.toFloat() / LOCKOUT_MILLIS.toFloat()).coerceIn(0f, 1f)
                    enterLocked(secondsLeft, progress)
                } else {
                    if (pendingCongrats) {
                        _uiState.value = KickUiState.Congrats(totalKicks.coerceAtLeast(1), totalKicks, history)
                    } else {
                        _uiState.value = KickUiState.Ready(totalKicks, history, firstKickTime, lastKickTime)
                    }
                    break
                }
            }
        }
    }

    /**
     * Persist a kick SYNCHRONOUSLY and update in-memory state.
     * Returns the kick timestamp. Callers should immediately finish() the Activity.
     */
    fun persistKickSync(): Long {
        val ctx = getApplication<Application>()
        val kickTime = System.currentTimeMillis()
        val newTotal = totalKicks + 1
        val resolvedFirst = if (firstKickTime == 0L) kickTime else firstKickTime
        KickPrefs.saveKickSync(ctx, kickTime, newTotal, history, firstKickTime)
        totalKicks = newTotal
        lastKickTime = kickTime
        pendingCongrats = true
        firstKickTime = resolvedFirst
        history = (listOf(kickTime) + history).sortedDescending().take(50)
        tickerJob?.cancel()
        enterLocked(LOCKOUT_SECONDS, 0f)
        startTicker()
        return kickTime
    }

    fun acknowledgeCongrats() {
        KickPrefs.clearPendingSync(getApplication())
        pendingCongrats = false
        tickerJob?.cancel()
        _uiState.value = KickUiState.Ready(totalKicks, history, firstKickTime, lastKickTime)
    }

    fun resetAll() {
        tickerJob?.cancel()
        KickPrefs.resetSync(getApplication())
        lastKickTime = 0L
        pendingCongrats = false
        totalKicks = 0
        history = emptyList()
        firstKickTime = 0L
        _uiState.value = KickUiState.Ready(0, emptyList(), 0L, 0L)
    }

    fun tauntFor(secondsLeft: Int): String {
        if (taunts.isEmpty()) return "Stay out!"
        val idx = (LOCKOUT_SECONDS - secondsLeft).coerceAtLeast(0) % taunts.size
        return taunts[idx]
    }

    fun congratsTitle(kickNumber: Int): String {
        return when {
            kickNumber <= 1 -> "First exile survived!"
            kickNumber in 2..4 -> "Exile #$kickNumber crushed!"
            kickNumber in 5..9 -> "Certified exile enjoyer!"
            kickNumber in 10..24 -> "10+ exiles?! Unstoppable!"
            kickNumber in 25..49 -> "Exile veteran #$kickNumber"
            else -> "ULTIMATE OUTCAST #$kickNumber"
        }
    }

    fun rankFor(kicks: Int): String {
        return when {
            kicks <= 0 -> "Rookie"
            kicks == 1 -> "Exile Survivor"
            kicks in 2..4 -> "Repeat Offender"
            kicks in 5..9 -> "Exile Enjoyer"
            kicks in 10..24 -> "Outcast Pro"
            kicks in 25..49 -> "Veteran Outcast"
            else -> "Ultimate Outcast"
        }
    }
}
