package com.example.socialbaby.util

import android.app.Activity

object ScreenPinningManager {
    fun startLockTask(activity: Activity) {
        try {
            activity.startLockTask()
        } catch (e: Exception) { }
    }

    fun stopLockTask(activity: Activity) {
        try {
            activity.stopLockTask()
        } catch (e: Exception) { }
    }

    fun isLocked(activity: Activity): Boolean {
        return try {
            val am = activity.getSystemService(android.app.ActivityManager::class.java)
            am?.isInLockTaskMode ?: false
        } catch (e: Exception) { false }
    }
}
