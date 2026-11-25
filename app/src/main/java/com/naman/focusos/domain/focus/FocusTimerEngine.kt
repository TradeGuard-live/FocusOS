package com.naman.focusos.domain.focus

import android.os.SystemClock
import com.naman.focusos.data.model.FocusPauseEntity
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.SessionStatus

data class LiveTimerSnapshot(
    val sessionId: Long,
    val status: SessionStatus,
    val focusedSeconds: Long,
    val pausedSeconds: Long,
    val totalElapsedSeconds: Long,
    val distractedSeconds: Long,
    val productiveSeconds: Long,
    val targetDurationSeconds: Long,
    val remainingSeconds: Long,
    val progress: Float,
    val distractionCount: Int,
    val appSwitchCount: Int
) {
    val isTargetCompleted: Boolean
        get() = targetDurationSeconds > 0 && focusedSeconds >= targetDurationSeconds

    val focusCompletionPercentage: Int
        get() = when {
            targetDurationSeconds <= 0 -> 100
            else -> ((focusedSeconds.toDouble() / targetDurationSeconds.toDouble()) * 100)
                .toInt()
                .coerceAtMost(100)
        }

    val formattedFocusedTime: String
        get() = formatDuration(focusedSeconds)

    val formattedRemainingTime: String
        get() = formatDuration(remainingSeconds)

    val formattedPausedTime: String
        get() = formatDuration(pausedSeconds)

    val formattedTotalElapsed: String
        get() = formatDuration(totalElapsedSeconds)

    companion object {
        fun formatDuration(totalSecs: Long): String {
            val secs = totalSecs.coerceAtLeast(0)
            val hours = secs / 3600
            val minutes = (secs % 3600) / 60
            val seconds = secs % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

        val IDLE = LiveTimerSnapshot(
            sessionId = 0L,
            status = SessionStatus.IDLE,
            focusedSeconds = 0L,
            pausedSeconds = 0L,
            totalElapsedSeconds = 0L,
            distractedSeconds = 0L,
            productiveSeconds = 0L,
            targetDurationSeconds = 0L,
            remainingSeconds = 0L,
            progress = 0f,
            distractionCount = 0,
            appSwitchCount = 0
        )
    }
}

class FocusTimerEngine(
    private val clockProvider: ClockProvider = SystemClockProvider
) {
    interface ClockProvider {
        fun elapsedRealtime(): Long
        fun currentTimeMillis(): Long
    }

    object SystemClockProvider : ClockProvider {
        override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
        override fun currentTimeMillis(): Long = System.currentTimeMillis()
    }

    var activeSessionId: Long = 0L
        private set

    var status: SessionStatus = SessionStatus.IDLE
        private set

    var sessionStartWallTime: Long = 0L
        private set

    private var activeSegmentStartElapsed: Long = 0L
    private var currentPauseStartElapsed: Long = 0L

    var accumulatedFocusedSeconds: Long = 0L
        private set

    var accumulatedPausedSeconds: Long = 0L
        private set

    var accumulatedDistractedSeconds: Long = 0L
        private set

    var accumulatedProductiveSeconds: Long = 0L
        private set

    var targetDurationSeconds: Long = 0L
        private set

    var distractionCount: Int = 0
        private set

    var appSwitchCount: Int = 0
        private set

    fun start(
        sessionId: Long,
        targetDurationSecs: Long,
        startWallTime: Long = clockProvider.currentTimeMillis(),
        startElapsed: Long = clockProvider.elapsedRealtime()
    ) {
        activeSessionId = sessionId
        status = SessionStatus.ACTIVE
        sessionStartWallTime = startWallTime
        activeSegmentStartElapsed = startElapsed
        currentPauseStartElapsed = 0L
        accumulatedFocusedSeconds = 0L
        accumulatedPausedSeconds = 0L
        accumulatedDistractedSeconds = 0L
        accumulatedProductiveSeconds = 0L
        targetDurationSeconds = targetDurationSecs
        distractionCount = 0
        appSwitchCount = 0
    }

    fun pause(
        pauseWallTime: Long = clockProvider.currentTimeMillis(),
        pauseElapsed: Long = clockProvider.elapsedRealtime()
    ): Long {
        if (status != SessionStatus.ACTIVE) return 0L
        val activeSegmentDuration = ((pauseElapsed - activeSegmentStartElapsed) / 1000).coerceAtLeast(0)
        accumulatedFocusedSeconds += activeSegmentDuration
        currentPauseStartElapsed = pauseElapsed
        status = SessionStatus.PAUSED
        return pauseWallTime
    }

    fun resume(
        resumeWallTime: Long = clockProvider.currentTimeMillis(),
        resumeElapsed: Long = clockProvider.elapsedRealtime()
    ): Long {
        if (status != SessionStatus.PAUSED) return 0L
        val pauseDuration = ((resumeElapsed - currentPauseStartElapsed) / 1000).coerceAtLeast(0)
        accumulatedPausedSeconds += pauseDuration
        activeSegmentStartElapsed = resumeElapsed
        status = SessionStatus.ACTIVE
        return resumeWallTime
    }

    fun stop(
        stopWallTime: Long = clockProvider.currentTimeMillis(),
        stopElapsed: Long = clockProvider.elapsedRealtime()
    ) {
        if (status == SessionStatus.ACTIVE) {
            val finalActiveDuration = ((stopElapsed - activeSegmentStartElapsed) / 1000).coerceAtLeast(0)
            accumulatedFocusedSeconds += finalActiveDuration
        } else if (status == SessionStatus.PAUSED) {
            val finalPauseDuration = ((stopElapsed - currentPauseStartElapsed) / 1000).coerceAtLeast(0)
            accumulatedPausedSeconds += finalPauseDuration
        }
        status = SessionStatus.STOPPED
    }

    fun complete(
        completeWallTime: Long = clockProvider.currentTimeMillis(),
        completeElapsed: Long = clockProvider.elapsedRealtime()
    ) {
        if (status == SessionStatus.ACTIVE) {
            val finalActiveDuration = ((completeElapsed - activeSegmentStartElapsed) / 1000).coerceAtLeast(0)
            accumulatedFocusedSeconds += finalActiveDuration
        } else if (status == SessionStatus.PAUSED) {
            val finalPauseDuration = ((completeElapsed - currentPauseStartElapsed) / 1000).coerceAtLeast(0)
            accumulatedPausedSeconds += finalPauseDuration
        }
        status = SessionStatus.COMPLETED
    }

    fun addDistraction(durationSeconds: Long, isDistracting: Boolean, isProductive: Boolean) {
        appSwitchCount++
        if (isDistracting) {
            distractionCount++
            accumulatedDistractedSeconds += durationSeconds
        } else if (isProductive) {
            accumulatedProductiveSeconds += durationSeconds
        }
    }

    fun getSnapshot(
        currentElapsed: Long = clockProvider.elapsedRealtime(),
        currentWall: Long = clockProvider.currentTimeMillis()
    ): LiveTimerSnapshot {
        if (status == SessionStatus.IDLE) return LiveTimerSnapshot.IDLE

        val currentSegmentFocused = if (status == SessionStatus.ACTIVE) {
            ((currentElapsed - activeSegmentStartElapsed) / 1000).coerceAtLeast(0)
        } else 0L

        val currentSegmentPaused = if (status == SessionStatus.PAUSED) {
            ((currentElapsed - currentPauseStartElapsed) / 1000).coerceAtLeast(0)
        } else 0L

        val liveFocused = accumulatedFocusedSeconds + currentSegmentFocused
        val livePaused = accumulatedPausedSeconds + currentSegmentPaused
        val liveElapsed = ((currentWall - sessionStartWallTime) / 1000).coerceAtLeast(0)

        val remaining = if (targetDurationSeconds > 0) {
            (targetDurationSeconds - liveFocused).coerceAtLeast(0)
        } else 0L

        val progress = if (targetDurationSeconds > 0) {
            (liveFocused.toFloat() / targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

        return LiveTimerSnapshot(
            sessionId = activeSessionId,
            status = status,
            focusedSeconds = liveFocused,
            pausedSeconds = livePaused,
            totalElapsedSeconds = liveElapsed,
            distractedSeconds = accumulatedDistractedSeconds,
            productiveSeconds = accumulatedProductiveSeconds,
            targetDurationSeconds = targetDurationSeconds,
            remainingSeconds = remaining,
            progress = progress,
            distractionCount = distractionCount,
            appSwitchCount = appSwitchCount
        )
    }

    /**
     * Recovers a session from database entity state (e.g., after process kill or reboot).
     * Calculates time faithfully without inventing unearned focus time.
     */
    fun recoverSession(
        session: FocusSessionEntity,
        pauses: List<FocusPauseEntity>,
        currentWall: Long = clockProvider.currentTimeMillis(),
        currentElapsed: Long = clockProvider.elapsedRealtime()
    ) {
        activeSessionId = session.id
        status = session.sessionStatus
        sessionStartWallTime = session.startTimestamp
        targetDurationSeconds = session.targetDurationSeconds
        accumulatedFocusedSeconds = session.focusedDurationSeconds
        accumulatedPausedSeconds = session.pausedDurationSeconds
        accumulatedDistractedSeconds = session.distractedDurationSeconds
        accumulatedProductiveSeconds = session.productiveAppDurationSeconds
        distractionCount = session.distractionCount
        appSwitchCount = session.appSwitchCount

        // Check if there was an open pause
        val openPause = pauses.find { it.resumeTimestamp == null }
        if (openPause != null) {
            status = SessionStatus.PAUSED
            // Accurately account for time spent in pause prior to recovery
            val elapsedPauseSecs = ((currentWall - openPause.pauseTimestamp) / 1000).coerceAtLeast(0)
            currentPauseStartElapsed = currentElapsed - (elapsedPauseSecs * 1000)
        } else if (session.sessionStatus == SessionStatus.PAUSED) {
            status = SessionStatus.PAUSED
            currentPauseStartElapsed = currentElapsed
        } else {
            status = session.sessionStatus
            activeSegmentStartElapsed = currentElapsed
        }
    }

    fun reset() {
        activeSessionId = 0L
        status = SessionStatus.IDLE
        sessionStartWallTime = 0L
        activeSegmentStartElapsed = 0L
        currentPauseStartElapsed = 0L
        accumulatedFocusedSeconds = 0L
        accumulatedPausedSeconds = 0L
        accumulatedDistractedSeconds = 0L
        accumulatedProductiveSeconds = 0L
        targetDurationSeconds = 0L
        distractionCount = 0
        appSwitchCount = 0
    }
}
