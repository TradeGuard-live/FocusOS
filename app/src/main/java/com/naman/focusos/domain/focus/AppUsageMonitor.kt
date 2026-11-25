package com.naman.focusos.domain.focus

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.SessionEventType
import com.naman.focusos.data.model.SessionStatus

data class RawUsageInterval(
    val packageName: String,
    val applicationName: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Long
)

interface AppUsageProvider {
    fun hasPermission(): Boolean
    fun queryEvents(startTime: Long, endTime: Long): List<RawUsageInterval>
    fun getApplicationLabel(packageName: String): String
}

class AndroidAppUsageProvider(private val context: Context) : AppUsageProvider {

    private val packageManager: PackageManager = context.packageManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    override fun hasPermission(): Boolean {
        if (appOpsManager == null) return false
        val mode = try {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } catch (e: Exception) {
            AppOpsManager.MODE_DEFAULT
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun getApplicationLabel(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    override fun queryEvents(startTime: Long, endTime: Long): List<RawUsageInterval> {
        if (!hasPermission() || usageStatsManager == null || startTime >= endTime) {
            return emptyList()
        }

        val intervals = mutableListOf<RawUsageInterval>()
        try {
            // Look back up to 10 minutes prior to startTime to capture the active foreground app
            // even if its launch event happened before the current query window.
            val lookbackStart = (startTime - 10 * 60 * 1000L).coerceAtLeast(0L)
            val events = usageStatsManager.queryEvents(lookbackStart, endTime)
            val event = UsageEvents.Event()

            var activePkg: String? = null
            var activeStart: Long = startTime

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val eventTime = event.timeStamp
                val pkg = event.packageName ?: continue

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (eventTime < startTime) {
                            activePkg = pkg
                            activeStart = startTime
                        } else {
                            if (activePkg != null && activePkg != pkg) {
                                val duration = (eventTime - activeStart) / 1000
                                if (duration > 0) {
                                    intervals.add(
                                        RawUsageInterval(
                                            packageName = activePkg,
                                            applicationName = getApplicationLabel(activePkg),
                                            startTimestamp = activeStart,
                                            endTimestamp = eventTime,
                                            durationSeconds = duration
                                        )
                                    )
                                }
                            }
                            activePkg = pkg
                            activeStart = eventTime
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        if (activePkg == pkg) {
                            if (eventTime >= startTime) {
                                val duration = (eventTime - activeStart) / 1000
                                if (duration > 0) {
                                    intervals.add(
                                        RawUsageInterval(
                                            packageName = activePkg,
                                            applicationName = getApplicationLabel(activePkg),
                                            startTimestamp = activeStart,
                                            endTimestamp = eventTime,
                                            durationSeconds = duration
                                        )
                                    )
                                }
                            }
                            activePkg = null
                        }
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_SHOWN -> {
                        if (activePkg != null && eventTime >= startTime) {
                            val duration = (eventTime - activeStart) / 1000
                            if (duration > 0) {
                                intervals.add(
                                    RawUsageInterval(
                                        packageName = activePkg,
                                        applicationName = getApplicationLabel(activePkg),
                                        startTimestamp = activeStart,
                                        endTimestamp = eventTime,
                                        durationSeconds = duration
                                    )
                                )
                            }
                        }
                        activePkg = null
                    }
                }
            }

            // Close trailing interval up to endTime if still foregrounded
            if (activePkg != null) {
                val duration = (endTime - activeStart) / 1000
                if (duration > 0) {
                    intervals.add(
                        RawUsageInterval(
                            packageName = activePkg,
                            applicationName = getApplicationLabel(activePkg),
                            startTimestamp = activeStart,
                            endTimestamp = endTime,
                            durationSeconds = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Android platform security or internal exception: return collected intervals
        }

        return intervals
    }

    companion object {
        fun createUsageAccessIntent(): Intent {
            return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}

class AppUsageMonitor(
    private val provider: AppUsageProvider,
    private val ourPackageName: String = "com.naman.focusos"
) {

    fun hasUsagePermission(): Boolean = provider.hasPermission()

    /**
     * Inspects raw usage events during a session checkpoint window and classifies them.
     * Distinguishes:
     * - ACTIVE session distraction: usage outside Focus OS while status == ACTIVE
     * - PAUSED session usage: usage outside Focus OS while status == PAUSED
     */
    fun processUsageWindow(
        sessionId: Long,
        windowStart: Long,
        windowEnd: Long,
        sessionState: SessionStatus,
        knownClassifications: Map<String, AppClassification>
    ): List<FocusAppUsageEntity> {
        if (!provider.hasPermission() || windowStart >= windowEnd) {
            return emptyList()
        }

        val rawIntervals = provider.queryEvents(windowStart, windowEnd)
        val result = mutableListOf<FocusAppUsageEntity>()

        for (interval in rawIntervals) {
            // Focus OS foreground is not a distraction
            if (interval.packageName == ourPackageName) continue

            val configured = knownClassifications[interval.packageName]
            val classification = when {
                sessionState == SessionStatus.PAUSED -> AppClassification.PAUSED_SESSION_USAGE
                configured != null -> configured
                isSystemPackage(interval.packageName) -> AppClassification.SYSTEM_APP
                else -> AppClassification.DISTRACTING_APP
            }

            result.add(
                FocusAppUsageEntity(
                    sessionId = sessionId,
                    packageName = interval.packageName,
                    applicationName = interval.applicationName,
                    classification = classification,
                    startTimestamp = interval.startTimestamp,
                    endTimestamp = interval.endTimestamp,
                    durationSeconds = interval.durationSeconds,
                    sessionState = if (sessionState == SessionStatus.PAUSED) "PAUSED" else "ACTIVE",
                    monitoringConfidence = "HIGH"
                )
            )
        }

        return result
    }

    private fun isSystemPackage(packageName: String): Boolean {
        return packageName.startsWith("android") ||
                packageName.startsWith("com.android.") ||
                packageName.startsWith("com.google.android.inputmethod") ||
                packageName.contains("systemui") ||
                packageName.contains("launcher")
    }
}
