package com.example.ccounter

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

const val EXTRA_OPEN_ROUTE = "ccounter_open_route"

private const val ACTION_REMINDER = "com.example.ccounter.ACTION_REMINDER"
private const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
private const val CHANNEL_DEFAULT = "ccounter_reminders_default"
private const val WEIGHT_WEEKLY_REQUEST_CODE_BASE = 20_000

enum class ReminderType(
    val requestCode: Int,
    val notificationId: Int,
) {
    BREAKFAST(1001, 2001),
    LUNCH(1002, 2002),
    DINNER(1003, 2003),
    WEIGHT(1004, 2004),
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val typeName = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: return
        val type = runCatching { ReminderType.valueOf(typeName) }.getOrNull() ?: return

        val settings = AppStorage(context).load().notifications
        if (!ReminderScheduler.shouldSchedule(type, settings)) return

        ReminderNotifier.show(context, type)
        ReminderScheduler.scheduleNext(context, type, settings)
    }
}

object ReminderScheduler {
    fun rescheduleAll(context: Context, settings: NotificationSettings) {
        ReminderNotifier.ensureChannels(context)
        cancelAll(context)
        scheduleEnabled(context, settings)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ReminderType.entries.forEach { type ->
            if (type == ReminderType.WEIGHT) {
                cancelWeightAlarms(context, alarmManager)
            } else {
                alarmManager.cancel(pendingBroadcast(context, type))
            }
        }
    }

    fun shouldSchedule(type: ReminderType, settings: NotificationSettings): Boolean {
        if (type == ReminderType.WEIGHT) {
            return settings.weightReminderEnabled
        }
        if (!settings.mealRemindersEnabled) return false
        return activeMealTypes(settings.mealsPerDay).contains(type)
    }

    fun scheduleNext(context: Context, type: ReminderType, settings: NotificationSettings) {
        if (type == ReminderType.WEIGHT) {
            scheduleWeightReminder(context, settings)
            return
        }
        if (!shouldSchedule(type, settings)) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingBroadcast(context, type))
            return
        }
        val triggerAtMillis = nextTriggerMillis(type, settings) ?: return
        scheduleAlarm(context, pendingBroadcast(context, type), triggerAtMillis)
    }

    private fun scheduleEnabled(context: Context, settings: NotificationSettings) {
        ReminderType.entries.forEach { type ->
            scheduleNext(context, type, settings)
        }
    }

    private fun scheduleAlarm(context: Context, pendingIntent: PendingIntent, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun scheduleWeightReminder(context: Context, settings: NotificationSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelWeightAlarms(context, alarmManager)
        if (!settings.weightReminderEnabled) return

        val zone = ZoneId.systemDefault()
        val now = Instant.now().atZone(zone)
        when (settings.weightFrequency) {
            WeightReminderFrequency.DAILY -> {
                val triggerAtMillis = nextDailyTime(settings.weightTimeMinutes, now).toInstant().toEpochMilli()
                scheduleAlarm(context, pendingWeightBroadcast(context), triggerAtMillis)
            }
            WeightReminderFrequency.WEEKLY -> {
                val selectedDays = settings.weightWeekDays
                    .distinct()
                    .ifEmpty { listOf(WeekDay.MON) }
                selectedDays.forEach { day ->
                    val triggerAtMillis = nextWeeklyTime(
                        day = day.toJavaDayOfWeek(),
                        minutes = settings.weightTimeMinutes,
                        from = now,
                    ).toInstant().toEpochMilli()
                    scheduleAlarm(context, pendingWeightBroadcast(context, day), triggerAtMillis)
                }
            }
            WeightReminderFrequency.CUSTOM -> {
                val triggerAtMillis = nextCustomTime(
                    everyDays = settings.weightCustomEveryDays.coerceIn(2, 30),
                    minutes = settings.weightTimeMinutes,
                    from = now,
                ).toInstant().toEpochMilli()
                scheduleAlarm(context, pendingWeightBroadcast(context), triggerAtMillis)
            }
        }
    }

    private fun nextTriggerMillis(type: ReminderType, settings: NotificationSettings): Long? {
        val zone = ZoneId.systemDefault()
        val now = Instant.now().atZone(zone)

        return when (type) {
            ReminderType.BREAKFAST -> nextDailyTime(settings.breakfastMinutes, now).toInstant().toEpochMilli()
            ReminderType.LUNCH -> nextDailyTime(settings.lunchMinutes, now).toInstant().toEpochMilli()
            ReminderType.DINNER -> nextDailyTime(settings.dinnerMinutes, now).toInstant().toEpochMilli()
            ReminderType.WEIGHT -> {
                when (settings.weightFrequency) {
                    WeightReminderFrequency.DAILY -> nextDailyTime(settings.weightTimeMinutes, now)
                    WeightReminderFrequency.WEEKLY -> return null
                    WeightReminderFrequency.CUSTOM -> nextCustomTime(
                        everyDays = settings.weightCustomEveryDays.coerceIn(2, 30),
                        minutes = settings.weightTimeMinutes,
                        from = now,
                    )
                }.toInstant().toEpochMilli()
            }
        }
    }

    private fun nextDailyTime(minutes: Int, from: java.time.ZonedDateTime): java.time.ZonedDateTime {
        val safeMinutes = minutes.coerceIn(0, 23 * 60 + 59)
        val hour = safeMinutes / 60
        val minute = safeMinutes % 60
        var candidate = from.toLocalDate().atTime(hour, minute).atZone(from.zone)
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun nextWeeklyTime(
        day: DayOfWeek,
        minutes: Int,
        from: java.time.ZonedDateTime,
    ): java.time.ZonedDateTime {
        val safeMinutes = minutes.coerceIn(0, 23 * 60 + 59)
        val hour = safeMinutes / 60
        val minute = safeMinutes % 60

        var candidate = from.toLocalDate()
            .with(TemporalAdjusters.nextOrSame(day))
            .atTime(hour, minute)
            .atZone(from.zone)
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusWeeks(1)
        }
        return candidate
    }

    private fun nextCustomTime(
        everyDays: Int,
        minutes: Int,
        from: java.time.ZonedDateTime,
    ): java.time.ZonedDateTime {
        val safeMinutes = minutes.coerceIn(0, 23 * 60 + 59)
        val hour = safeMinutes / 60
        val minute = safeMinutes % 60
        var candidate = from.toLocalDate().atTime(hour, minute).atZone(from.zone)
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusDays(everyDays.toLong())
        }
        return candidate
    }

    private fun activeMealTypes(mealsPerDay: Int): List<ReminderType> = when (mealsPerDay.coerceIn(1, 3)) {
        1 -> listOf(ReminderType.BREAKFAST)
        2 -> listOf(ReminderType.BREAKFAST, ReminderType.LUNCH)
        else -> listOf(ReminderType.BREAKFAST, ReminderType.LUNCH, ReminderType.DINNER)
    }

    private fun pendingBroadcast(context: Context, type: ReminderType): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, type.name)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, type.requestCode, intent, flags)
    }

    private fun pendingWeightBroadcast(context: Context, day: WeekDay? = null): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, ReminderType.WEIGHT.name)
        }
        val requestCode = if (day == null) {
            ReminderType.WEIGHT.requestCode
        } else {
            WEIGHT_WEEKLY_REQUEST_CODE_BASE + day.ordinal
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun cancelWeightAlarms(context: Context, alarmManager: AlarmManager) {
        alarmManager.cancel(pendingWeightBroadcast(context))
        WeekDay.entries.forEach { day ->
            alarmManager.cancel(pendingWeightBroadcast(context, day))
        }
    }
}

private object ReminderNotifier {
    private fun defaultSoundUri() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_DEFAULT) != null) return
        val channel = NotificationChannel(
            CHANNEL_DEFAULT,
            "CCounter reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Meal and weight reminders"
            enableVibration(true)
            setSound(
                defaultSoundUri(),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, type: ReminderType) {
        ensureChannels(context)

        val (title, body, route) = when (type) {
            ReminderType.BREAKFAST -> Triple(breakfastTitles.random(), "Send a photo of your breakfast", Routes.AddMeal)
            ReminderType.LUNCH -> Triple(lunchTitles.random(), "Send a photo of your lunch", Routes.AddMeal)
            ReminderType.DINNER -> Triple(dinnerTitles.random(), "Send a photo of your dinner", Routes.AddMeal)
            ReminderType.WEIGHT -> Triple(weightTitles.random(), "Update your current weight", Routes.Weight)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ROUTE, route)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            context,
            type.notificationId + 5000,
            contentIntent,
            pendingFlags,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DEFAULT)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(type.notificationId, notification)
    }

    private val breakfastTitles = listOf(
        "Good morning, time for something tasty 🍳",
        "Start your day with a nice meal ☀️",
        "Breakfast time, don’t skip it 🥐",
        "Treat yourself to a good morning bite 🍓",
        "A fresh start begins with breakfast 🥣",
        "Take a moment to enjoy your morning food 🍞",
        "Your morning energy starts here 🍌",
    )

    private val lunchTitles = listOf(
        "Time to take a break and eat something good 🥗",
        "Lunch is a perfect moment to recharge 🍜",
        "Don’t forget to enjoy your midday meal 🥪",
        "A good lunch keeps you going strong 🍛",
        "Take a pause and grab something tasty 🍝",
        "Give yourself a proper lunch break 🍗",
        "A little food boost for your day 🥙",
    )

    private val dinnerTitles = listOf(
        "Time to relax and enjoy your dinner 🍲",
        "End your day with something вкусное 🍖",
        "Dinner is ready when you are 🌙",
        "A calm evening starts with a good meal 🥩",
        "Take your time and enjoy dinner 🍜",
        "A warm meal to finish your day 🍛",
        "Make your evening a bit tastier 🍗",
    )

    private val weightTitles = listOf(
        "A small check-in for today ⚖️",
        "See how you’re doing today 📊",
        "A quick moment for yourself ⚖️",
        "Stay in touch with your progress 🧭",
        "Just a gentle reminder for today ⚖️",
        "Keep an eye on your journey 📉",
        "A simple step towards your goal ⚖️",
    )
}

private fun WeekDay.toJavaDayOfWeek(): DayOfWeek = when (this) {
    WeekDay.MON -> DayOfWeek.MONDAY
    WeekDay.TUE -> DayOfWeek.TUESDAY
    WeekDay.WED -> DayOfWeek.WEDNESDAY
    WeekDay.THU -> DayOfWeek.THURSDAY
    WeekDay.FRI -> DayOfWeek.FRIDAY
    WeekDay.SAT -> DayOfWeek.SATURDAY
    WeekDay.SUN -> DayOfWeek.SUNDAY
}
