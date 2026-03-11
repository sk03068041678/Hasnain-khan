package com.example.applocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LockService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var prefs: SharedPrefsHelper
    private var lastPackageName: String = ""

    override fun onCreate() {
        super.onCreate()
        prefs = SharedPrefsHelper(this)
        createNotificationChannel()
        startForeground(1, createNotification())
        startMonitoring()
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                val topPackage = getTopPackage()
                if (topPackage != null && topPackage != packageName) {
                    val lockedApps = prefs.getLockedApps()
                    val tempUnlocked = prefs.getTemporarilyUnlocked()

                    if (lockedApps.contains(topPackage)) {
                        if (tempUnlocked != topPackage) {
                            // App is locked and not temporarily unlocked
                            if (lastPackageName != topPackage) {
                                showLockScreen(topPackage)
                            }
                        }
                    } else {
                        // If user switches to an unlocked app, clear the temp unlock state
                        if (tempUnlocked != null && topPackage != "com.android.launcher" && topPackage != "com.google.android.apps.nexuslauncher") {
                            prefs.clearTemporarilyUnlocked()
                        }
                    }
                    lastPackageName = topPackage
                }
                delay(500) // Check every 500ms
            }
        }
    }

    private fun showLockScreen(packageName: String) {
        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("PACKAGE_NAME", packageName)
        }
        startActivity(intent)
    }

    private fun getTopPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)
        if (stats != null) {
            var topActivity: UsageStats? = null
            for (usageStats in stats) {
                if (topActivity == null || usageStats.lastTimeUsed > topActivity.lastTimeUsed) {
                    topActivity = usageStats
                }
            }
            return topActivity?.packageName
        }
        return null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "AppLockerChannel",
                "App Locker Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "AppLockerChannel")
            .setContentTitle("App Locker Running")
            .setContentText("Protecting your apps")
            .setSmallIcon(android.R.drawable.ic_secure)
            .build()
    }
}
