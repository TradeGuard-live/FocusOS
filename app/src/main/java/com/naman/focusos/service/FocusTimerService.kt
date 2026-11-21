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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusTimerService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: FocusSessionRepository
    private lateinit var usageMonitor: AppUsageMonitor
    private val timerEngine = FocusTimerEngine()

    private var activeSessionId: Long = 0L
    private var sessionTitle: String = "Focus Session"
    private var targetDurationSeconds: Long = 0L
    private var monitoringEnabled: Boolean = true

    private var lastUsageQueryTimestamp: Long = 0L
    private var classificationCache: Map<String, AppClassification> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        repository = FocusSessionRepository(applicationContext)
        usageMonitor = AppUsageMonitor(AndroidAppUsageProvider(applicationContext))
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

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

                timerEngine.start(
                    sessionId = sessionId,
                    targetDurationSecs = targetSecs
                )
                _activeSessionSnapshot.value = timerEngine.getSnapshot()

                val notification = buildNotification(timerEngine.getSnapshot())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                startTimerLoop()
            }

            ACTION_PAUSE -> {
                timerEngine.pause()
                _activeSessionSnapshot.value = timerEngine.getSnapshot()
                updateNotification()
                serviceScope.launch(Dispatchers.IO) {
                    repository.pauseSession(activeSessionId)
                }
            }

            ACTION_RESUME -> {
                timerEngine.resume()
                _activeSessionSnapshot.value = timerEngine.getSnapshot()
                updateNotification()
                serviceScope.launch(Dispatchers.IO) {
                    repository.resumeSession(activeSessionId)
                }
            }

            ACTION_STOP -> {
                val snapshot = timerEngine.getSnapshot()
                timerEngine.stop()
                _activeSessionSnapshot.value = LiveTimerSnapshot.IDLE
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
                serviceScope.launch(Dispatchers.IO) {
                    repository.completeSession(activeSessionId, snapshot, markTask)
                    stopServiceGracefully()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startTimerLoop() {
        serviceScope.launch {
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

                // Update notification every 1 second
                updateNotification()

                // Checkpoint to DB and check usage every 3 seconds
                if (tickCounter % 3 == 0) {
                    serviceScope.launch(Dispatchers.IO) {
                        repository.checkpointSession(activeSessionId, snapshot)

                        if (monitoringEnabled && usageMonitor.hasUsagePermission()) {
                            val now = System.currentTimeMillis()
                            val intervals = usageMonitor.processUsageWindow(
                                sessionId = activeSessionId,
                                windowStart = lastUsageQueryTimestamp,
                                windowEnd = now,
                                sessionState = timerEngine.status,
                                knownClassifications = classificationCache
                            )
                            if (intervals.isNotEmpty()) {
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

        val statusText = when (snapshot.status) {
            SessionStatus.ACTIVE -> {
                val focused = snapshot.formattedFocusedTime
                if (snapshot.targetDurationSeconds > 0) {
                    val target = LiveTimerSnapshot.formatDuration(snapshot.targetDurationSeconds)
                    "$focused / $target • Focused"
                } else {
                    "$focused • Focused"
                }
            }
            SessionStatus.PAUSED -> "${snapshot.formattedPausedTime} • Paused"
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
        serviceJob.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
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
