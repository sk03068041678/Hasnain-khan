package com.example.applocker

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AppLockerPrefs", Context.MODE_PRIVATE)

    fun savePin(pin: String) {
        prefs.edit().putString("PIN", pin).apply()
    }

    fun getPin(): String? {
        return prefs.getString("PIN", null)
    }

    fun lockApp(packageName: String) {
        val lockedApps = getLockedApps().toMutableSet()
        lockedApps.add(packageName)
        prefs.edit().putStringSet("LOCKED_APPS", lockedApps).apply()
    }

    fun unlockApp(packageName: String) {
        val lockedApps = getLockedApps().toMutableSet()
        lockedApps.remove(packageName)
        prefs.edit().putStringSet("LOCKED_APPS", lockedApps).apply()
    }

    fun getLockedApps(): Set<String> {
        return prefs.getStringSet("LOCKED_APPS", emptySet()) ?: emptySet()
    }
    
    fun setTemporarilyUnlocked(packageName: String) {
        prefs.edit().putString("TEMP_UNLOCKED", packageName).apply()
        prefs.edit().putLong("TEMP_UNLOCKED_TIME", System.currentTimeMillis()).apply()
    }
    
    fun setTimeoutDuration(durationInMillis: Long) {
        prefs.edit().putLong("TIMEOUT_DURATION", durationInMillis).apply()
    }

    fun getTimeoutDuration(): Long {
        return prefs.getLong("TIMEOUT_DURATION", 60000L) // Default to 1 minute
    }
    
    fun getTemporarilyUnlocked(): String? {
        val time = prefs.getLong("TEMP_UNLOCKED_TIME", 0)
        val timeout = getTimeoutDuration()
        if (System.currentTimeMillis() - time > timeout) {
            return null
        }
        return prefs.getString("TEMP_UNLOCKED", null)
    }
    
    fun clearTemporarilyUnlocked() {
        prefs.edit().remove("TEMP_UNLOCKED").remove("TEMP_UNLOCKED_TIME").apply()
    }
}
