package com.example.socialbaby.domain.usecase

import com.example.socialbaby.data.local.dao.WatchLogDao
import com.example.socialbaby.domain.model.ParentSettings
import java.util.Calendar
import javax.inject.Inject

class EnforceTimeLimitUseCase @Inject constructor(
    private val watchLogDao: WatchLogDao
) {
    suspend operator fun invoke(settings: ParentSettings): TimeLimitResult {
        // Check bedtime window
        if (settings.bedtimeEnabled && isInBedtime(settings.bedtimeStart, settings.bedtimeEnd)) {
            return TimeLimitResult.BlockedBedtime
        }
        // Check daily limit
        if (settings.dailyLimitMinutes > 0) {
            val sinceMidnight = startOfToday()
            val total = watchLogDao.totalWatchSince(sinceMidnight) ?: 0L
            val limitMs = settings.dailyLimitMinutes * 60L * 1000L
            if (total >= limitMs) {
                return TimeLimitResult.BlockedDailyLimit(usedMs = total, limitMs = limitMs)
            }
            val remaining = limitMs - total
            if (remaining < 5 * 60 * 1000) {
                return TimeLimitResult.WarningAlmostUp(remainingMs = remaining)
            }
        }
        return TimeLimitResult.Allowed
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun isInBedtime(start: String, end: String): Boolean {
        return try {
            val (sh, sm) = start.split(":").map { it.toInt() }
            val (eh, em) = end.split(":").map { it.toInt() }
            val now = Calendar.getInstance()
            val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val startM = sh * 60 + sm
            val endM = eh * 60 + em
            if (startM <= endM) {
                nowMinutes in startM..endM
            } else {
                nowMinutes >= startM || nowMinutes <= endM
            }
        } catch (e: Exception) { false }
    }

    sealed class TimeLimitResult {
        data object Allowed : TimeLimitResult()
        data object BlockedBedtime : TimeLimitResult()
        data class BlockedDailyLimit(val usedMs: Long, val limitMs: Long) : TimeLimitResult()
        data class WarningAlmostUp(val remainingMs: Long) : TimeLimitResult()
    }
}
