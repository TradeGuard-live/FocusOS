package com.naman.focusos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.naman.focusos.MainActivity
import com.naman.focusos.R
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.MonitoringStatus
import com.naman.focusos.data.model.SessionStatus
import com.naman.focusos.data.repository.FocusSessionRepository
import com.naman.focusos.domain.focus.AndroidAppUsageProvider
import com.naman.focusos.domain.focus.AppUsageMonitor
import com.naman.focusos.domain.focus.FocusTimerEngine
import com.naman.focusos.domain.focus.LiveTimerSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocusTimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var repository: FocusSessionRepository
    private lateinit var usageMonitor: AppUsageMonitor
    private val timerEngine = FocusTimerEngine()

    private var activeSessionId: Long = 0L
    private var sessionTitle: String = "Focus Session"
    private var targetDurationSeconds: Long = 0L
    private var monitoringEnabled: Boolean = true

    private var lastUsageQueryTimestamp: Long = 0L
    private var classificationCache: Map<String, AppClassification> = emptyMap()
    private var lastDetectedAppName: String? = null

    private var timerLoopJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        repository = FocusSessionRepository(applicationContext)
        usageMonitor = AppUsageMonitor(AndroidAppUsageProvider(applicationContext))
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == null || action == ACTION_RECOVER) {
            // Android requirement: If started as a foreground service, establish foreground notification immediately
            val initialNotification = buildNotification(_activeSessionSnapshot.value)
            startForegroundServiceCompat(initialNotification)
            val requestedSessionId = intent?.getLongExtra(EXTRA_SESSION_ID, 0L) ?: 0L
            handleRecover(requestedSessionId)
            return START_STICKY
        }

        when (action) {
            ACTION_START -> {
                val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
                val title = intent.getStringExtra(EXTRA_SESSION_TITLE) ?: "Focus Session"
                val targetSecs = intent.getLongExtra(EXTRA_TARGET_DURATION, 0L)
                val monitor = intent.getBooleanExtra(EXTRA_MONITORING_ENABLED, true)

                activeSessionId = sessionId
                sessionTitle = title
                targetDurationSeconds = targetSecs
                monitoringEnabled = monitor
                lastUsageQueryTimestamp = System.currentTimeMillis()
                lastDetectedAppName = null

                timerEngine.start(
                    sessionId = sessionId,
                    targetDurationSecs = targetSecs
                )
                val snapshot = timerEngine.getSnapshot()
                _activeSessionSnapshot.value = snapshot

                acquireWakeLock()
                val notification = buildNotification(snapshot)
                startForegroundServiceCompat(notification)
                startTimerLoop()
            }

            ACTION_PAUSE -> {
                handlePause()
            }

            ACTION_RESUME -> {
                handleResume()
            }

            ACTION_STOP -> {
                val snapshot = timerEngine.getSnapshot()
                timerEngine.stop()
                _activeSessionSnapshot.value = LiveTimerSnapshot.IDLE
                releaseWakeLock()
                serviceScope.launch(Dispatchers.IO) {
                    repository.stopSession(activeSessionId, snapshot)
                    stopServiceGracefully()
                }
            }

            ACTION_COMPLETE -> {
                val snapshot = timerEngine.getSnapshot()
                val markTask = intent.getBooleanExtra(EXTRA_MARK_TASK_COMPLETED, false)
                timerEngine.complete()
                _activeSessionSnapshot.value = LiveTimerSnapshot.IDLE
                releaseWakeLock()
                serviceScope.launch(Dispatchers.IO) {
                    repository.completeSession(activeSessionId, snapshot, markTask)
                    stopServiceGracefully()
                }
            }
        }

        return START_STICKY
    }

    private fun handlePause() {
        if (activeSessionId == 0L || timerEngine.status == SessionStatus.IDLE) {
            serviceScope.launch(Dispatchers.IO) {
                val session = repository.getActiveSession()
                if (session != null) {
                    val pauses = repository.getPausesForSessionSync(session.id)
                    withContext(Dispatchers.Default) {
                        activeSessionId = session.id
                        sessionTitle = session.title
                        targetDurationSeconds = session.targetDurationSeconds
                        monitoringEnabled = session.monitoringEnabled
                        timerEngine.recoverSession(session, pauses)
                        lastUsageQueryTimestamp = System.currentTimeMillis()
                        if (timerEngine.status == SessionStatus.ACTIVE) {
                            timerEngine.pause()
                        }
                        val snapshot = timerEngine.getSnapshot()
                        _activeSessionSnapshot.value = snapshot
                        updateNotification()
                        startTimerLoop()
                        releaseWakeLock()
                        repository.pauseSession(activeSessionId, snapshot)
                    }
                }
            }
        } else {
            timerEngine.pause()
            val snapshot = timerEngine.getSnapshot()
            _activeSessionSnapshot.value = snapshot
            updateNotification()
            releaseWakeLock()
            serviceScope.launch(Dispatchers.IO) {
                repository.pauseSession(activeSessionId, snapshot)
            }
        }
    }

    private fun handleResume() {
        acquireWakeLock()
        if (activeSessionId == 0L || timerEngine.status == SessionStatus.IDLE) {
            serviceScope.launch(Dispatchers.IO) {
                val session = repository.getActiveSession()
                if (session != null) {
                    val pauses = repository.getPausesForSessionSync(session.id)
                    withContext(Dispatchers.Default) {
                        activeSessionId = session.id
                        sessionTitle = session.title
                        targetDurationSeconds = session.targetDurationSeconds
                        monitoringEnabled = session.monitoringEnabled
                        timerEngine.recoverSession(session, pauses)
                        lastUsageQueryTimestamp = System.currentTimeMillis()
                        if (timerEngine.status == SessionStatus.PAUSED) {
                            timerEngine.resume()
                        }
                        val snapshot = timerEngine.getSnapshot()
                        _activeSessionSnapshot.value = snapshot
                        updateNotification()
                        startTimerLoop()
                        repository.resumeSession(activeSessionId, snapshot)
                    }
                }
            }
        } else {
            timerEngine.resume()
            val snapshot = timerEngine.getSnapshot()
            _activeSessionSnapshot.value = snapshot
            updateNotification()
            serviceScope.launch(Dispatchers.IO) {
                repository.resumeSession(activeSessionId, snapshot)
            }
            if (timerLoopJob?.isActive != true) {
                startTimerLoop()
            }
        }
    }

    private fun handleRecover(sessionId: Long = 0L) {
        serviceScope.launch(Dispatchers.IO) {
            val session = if (sessionId > 0L) repository.getSessionById(sessionId) else repository.getActiveSession()
            if (session != null && (session.sessionStatus == SessionStatus.ACTIVE || session.sessionStatus == SessionStatus.PAUSED)) {
                val pauses = repository.getPausesForSessionSync(session.id)
                withContext(Dispatchers.Default) {
                    activeSessionId = session.id
                    sessionTitle = session.title
                    targetDurationSeconds = session.targetDurationSeconds
                    monitoringEnabled = session.monitoringEnabled
                    timerEngine.recoverSession(session, pauses)
                    lastUsageQueryTimestamp = System.currentTimeMillis()
                    val snapshot = timerEngine.getSnapshot()
                    _activeSessionSnapshot.value = snapshot
                    if (timerEngine.status == SessionStatus.ACTIVE) {
                        acquireWakeLock()
                    } else {
                        releaseWakeLock()
                    }
                    val notification = buildNotification(snapshot)
                    startForegroundServiceCompat(notification)
                    startTimerLoop()
                }
            } else {
                withContext(Dispatchers.Default) {
                    stopServiceGracefully()
                }
            }
        }
    }

    private fun startForegroundServiceCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTimerLoop() {
        timerLoopJob?.cancel()
        timerLoopJob = serviceScope.launch(Dispatchers.IO) {
            // Load classifications once
            classificationCache = repository.getAllClassificationsSync().associate {
                it.packageName to it.classification
            }

            var tickCounter = 0
            while (isActive && (timerEngine.status == SessionStatus.ACTIVE || timerEngine.status == SessionStatus.PAUSED)) {
                delay(1000)
                tickCounter++

                val snapshot = timerEngine.getSnapshot()
                _activeSessionSnapshot.value = snapshot

                // Update notification every 1 second so pause and focus counts are live
                updateNotification()

                // Check app usage every 3 seconds; checkpoint session entity every 10 seconds (or on new usage)
                val checkUsage = (tickCounter % 3 == 0)
                val doCheckpoint = (tickCounter % 10 == 0)

                if (checkUsage || doCheckpoint) {
                    try {
                        var hadNewUsages = false
                        if (checkUsage && monitoringEnabled && usageMonitor.hasUsagePermission()) {
                            val now = System.currentTimeMillis()
                            val intervals = usageMonitor.processUsageWindow(
                                sessionId = activeSessionId,
                                windowStart = lastUsageQueryTimestamp,
                                windowEnd = now,
                                sessionState = timerEngine.status,
                                knownClassifications = classificationCache
                            )
                            if (intervals.isNotEmpty()) {
                                hadNewUsages = true
                                lastDetectedAppName = intervals.lastOrNull()?.applicationName
                                repository.recordAppUsages(activeSessionId, intervals)
                                for (interval in intervals) {
                                    val isDistraction = interval.classification == AppClassification.DISTRACTING_APP
                                    val isProductive = interval.classification == AppClassification.PRODUCTIVE_APP
                                    timerEngine.addDistraction(
                                        durationSeconds = interval.durationSeconds,
                                        isDistracting = isDistraction,
                                        isProductive = isProductive
                                    )
                                }
                            }
                            lastUsageQueryTimestamp = now
                        }

                        if (doCheckpoint || hadNewUsages) {
                            // Take snapshot AFTER processing usage metrics
                            val checkpointSnapshot = timerEngine.getSnapshot()
                            _activeSessionSnapshot.value = checkpointSnapshot
                            repository.checkpointSession(activeSessionId, checkpointSnapshot)
                        }
                    } catch (e: Exception) {
                        // Defensive: Prevent crash from interrupting the timer loop
                    }
                }
            }
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val snapshot = timerEngine.getSnapshot()
        val notification = buildNotification(snapshot)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(snapshot: LiveTimerSnapshot): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_FOCUS, true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appSuffix = if (!lastDetectedAppName.isNullOrBlank() &&
            lastDetectedAppName != "FocusOs" &&
            lastDetectedAppName != "Focus OS"
        ) " • Using $lastDetectedAppName" else ""

        val statusText = when (snapshot.status) {
            SessionStatus.ACTIVE -> {
                val focused = snapshot.formattedFocusedTime
                if (snapshot.targetDurationSeconds > 0) {
                    val target = LiveTimerSnapshot.formatDuration(snapshot.targetDurationSeconds)
                    "$focused / $target • Focused$appSuffix"
                } else {
                    "$focused • Focused$appSuffix"
                }
            }
            SessionStatus.PAUSED -> "${snapshot.formattedPausedTime} • Paused$appSuffix"
            else -> "Focus Session"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_focus_notification)
            .setContentTitle(sessionTitle)
            .setContentText(statusText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Actions: Pause / Resume & Stop
        if (snapshot.status == SessionStatus.ACTIVE) {
            val pauseIntent = Intent(this, FocusTimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePending = PendingIntent.getService(
                this, 1, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        } else if (snapshot.status == SessionStatus.PAUSED) {
            val resumeIntent = Intent(this, FocusTimerService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePending = PendingIntent.getService(
                this, 2, resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        }

        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_delete, "Stop", stopPending)

        return builder.build()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FocusOS:TimerServiceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 60 * 1000L) // 6 hours safety timeout
            }
        } catch (e: Exception) {
            // WakeLock acquisition error
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live focus session progress and control actions"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun stopServiceGracefully() {
        timerLoopJob?.cancel()
        serviceJob.cancel()
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        timerLoopJob?.cancel()
        serviceJob.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onTimeout(startId: Int) {
        // Android imposed a foreground service time limit; persist and shut down safely.
        serviceScope.launch(Dispatchers.IO) {
            try {
                val snapshot = timerEngine.getSnapshot()
                if (activeSessionId > 0L) {
                    repository.checkpointSession(activeSessionId, snapshot)
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                stopServiceGracefully()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "focus_session_timer_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.naman.focusos.ACTION_START_FOCUS"
        const val ACTION_PAUSE = "com.naman.focusos.ACTION_PAUSE_FOCUS"
        const val ACTION_RESUME = "com.naman.focusos.ACTION_RESUME_FOCUS"
        const val ACTION_STOP = "com.naman.focusos.ACTION_STOP_FOCUS"
        const val ACTION_COMPLETE = "com.naman.focusos.ACTION_COMPLETE_FOCUS"
        const val ACTION_RECOVER = "com.naman.focusos.ACTION_RECOVER_FOCUS"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_SESSION_TITLE = "extra_session_title"
        const val EXTRA_TARGET_DURATION = "extra_target_duration"
        const val EXTRA_MONITORING_ENABLED = "extra_monitoring_enabled"
        const val EXTRA_MARK_TASK_COMPLETED = "extra_mark_task_completed"
        const val EXTRA_NAVIGATE_TO_FOCUS = "extra_navigate_to_focus"

        private val _activeSessionSnapshot = MutableStateFlow(LiveTimerSnapshot.IDLE)
        val activeSessionSnapshot: StateFlow<LiveTimerSnapshot> = _activeSessionSnapshot.asStateFlow()

        fun startSession(
            context: Context,
            sessionId: Long,
            title: String,
            targetDurationSeconds: Long,
            monitoringEnabled: Boolean
        ) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_SESSION_TITLE, title)
                putExtra(EXTRA_TARGET_DURATION, targetDurationSeconds)
                putExtra(EXTRA_MONITORING_ENABLED, monitoringEnabled)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun recoverSession(context: Context, sessionId: Long = 0L) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_RECOVER
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseSession(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeSession(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopSession(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun completeSession(context: Context, markTaskCompleted: Boolean = false) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_COMPLETE
                putExtra(EXTRA_MARK_TASK_COMPLETED, markTaskCompleted)
            }
            context.startService(intent)
        }
    }
}
