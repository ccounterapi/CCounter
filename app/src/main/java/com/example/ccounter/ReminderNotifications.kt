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
private const val FULL_MEAL_KCAL_THRESHOLD = 300

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

        val appData = AppStorage(context).load()
        val settings = appData.notifications
        if (!ReminderScheduler.shouldSchedule(type, settings)) return

        if (type.isMealType() && hasCompletedMealForToday(appData.meals, type)) {
            ReminderScheduler.scheduleNext(context, type, settings)
            return
        }

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
    private fun tr(language: AppLanguage, en: String, ru: String, uk: String): String = when (language) {
        AppLanguage.ENGLISH -> en
        AppLanguage.RUSSIAN -> ru
        AppLanguage.UKRAINIAN -> uk
    }

    private fun defaultSoundUri() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun ensureChannels(context: Context) {
        val language = AppStorage(context).load().language
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_DEFAULT) != null) return
        val channel = NotificationChannel(
            CHANNEL_DEFAULT,
            tr(language, "CCounter reminders", "Напоминания CCounter", "Нагадування CCounter"),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = tr(language, "Meal and weight reminders", "Напоминания о еде и весе", "Нагадування про їжу та вагу")
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
        val language = AppStorage(context).load().language

        val (title, body, route) = when (type) {
            ReminderType.BREAKFAST -> Triple(
                breakfastTitles(language).random(),
                tr(language, "Send a photo of your breakfast", "Отправьте фото вашего завтрака", "Надішліть фото вашого сніданку"),
                Routes.AddMeal,
            )
            ReminderType.LUNCH -> Triple(
                lunchTitles(language).random(),
                tr(language, "Send a photo of your lunch", "Отправьте фото вашего обеда", "Надішліть фото вашого обіду"),
                Routes.AddMeal,
            )
            ReminderType.DINNER -> Triple(
                dinnerTitles(language).random(),
                tr(language, "Send a photo of your dinner", "Отправьте фото вашего ужина", "Надішліть фото вашої вечері"),
                Routes.AddMeal,
            )
            ReminderType.WEIGHT -> Triple(
                weightTitles(language).random(),
                tr(language, "Update your current weight", "Обновите ваш текущий вес", "Оновіть вашу поточну вагу"),
                Routes.Weight,
            )
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

    private fun breakfastTitles(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Good morning, time for something tasty 🍳",
            "Start your day with a nice meal ☀️",
            "Breakfast time, don’t skip it 🥐",
            "Treat yourself to a good morning bite 🍓",
            "A fresh start begins with breakfast 🥣",
            "Take a moment to enjoy your morning food 🍞",
            "Your morning energy starts here 🍌",
        )
        AppLanguage.RUSSIAN -> listOf(
            "Доброе утро, время чего-то вкусного 🍳",
            "Начните день с приятного приема пищи ☀️",
            "Время завтрака, не пропускайте его 🥐",
            "Порадуйте себя утренним перекусом 🍓",
            "Свежий старт начинается с завтрака 🥣",
            "Насладитесь утренней едой 🍞",
            "Ваша утренняя энергия начинается здесь 🍌",
        )
        AppLanguage.UKRAINIAN -> listOf(
            "Доброго ранку, час для чогось смачного 🍳",
            "Почніть день з приємного прийому їжі ☀️",
            "Час сніданку, не пропускайте його 🥐",
            "Потіште себе ранковим перекусом 🍓",
            "Свіжий старт починається зі сніданку 🥣",
            "Насолодіться ранковою їжею 🍞",
            "Ваша ранкова енергія починається тут 🍌",
        )
    }

    private fun lunchTitles(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Time to take a break and eat something good 🥗",
            "Lunch is a perfect moment to recharge 🍜",
            "Don’t forget to enjoy your midday meal 🥪",
            "A good lunch keeps you going strong 🍛",
            "Take a pause and grab something tasty 🍝",
            "Give yourself a proper lunch break 🍗",
            "A little food boost for your day 🥙",
        )
        AppLanguage.RUSSIAN -> listOf(
            "Время сделать паузу и поесть что-то вкусное 🥗",
            "Обед — отличный момент перезагрузиться 🍜",
            "Не забудьте про дневной прием пищи 🥪",
            "Хороший обед поддержит вашу энергию 🍛",
            "Сделайте паузу и перекусите вкусно 🍝",
            "Подарите себе полноценный обеденный перерыв 🍗",
            "Небольшой пищевой буст для вашего дня 🥙",
        )
        AppLanguage.UKRAINIAN -> listOf(
            "Час зробити паузу й зʼїсти щось смачне 🥗",
            "Обід — ідеальний момент перезавантажитися 🍜",
            "Не забудьте про денний прийом їжі 🥪",
            "Хороший обід підтримує вашу енергію 🍛",
            "Зробіть паузу та перекусіть смачно 🍝",
            "Подаруйте собі повноцінну обідню перерву 🍗",
            "Невеликий харчовий буст для вашого дня 🥙",
        )
    }

    private fun dinnerTitles(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Time to relax and enjoy your dinner 🍲",
            "End your day with something delicious 🍖",
            "Dinner is ready when you are 🌙",
            "A calm evening starts with a good meal 🥩",
            "Take your time and enjoy dinner 🍜",
            "A warm meal to finish your day 🍛",
            "Make your evening a bit tastier 🍗",
        )
        AppLanguage.RUSSIAN -> listOf(
            "Время расслабиться и насладиться ужином 🍲",
            "Завершите день чем-то вкусным 🍖",
            "Ужин готов, когда будете готовы вы 🌙",
            "Спокойный вечер начинается с хорошего ужина 🥩",
            "Не спешите и наслаждайтесь ужином 🍜",
            "Теплый прием пищи в завершение дня 🍛",
            "Сделайте вечер немного вкуснее 🍗",
        )
        AppLanguage.UKRAINIAN -> listOf(
            "Час розслабитися та насолодитися вечерею 🍲",
            "Завершіть день чимось смачним 🍖",
            "Вечеря готова, коли будете готові ви 🌙",
            "Спокійний вечір починається з хорошої вечері 🥩",
            "Не поспішайте та насолоджуйтеся вечерею 🍜",
            "Теплий прийом їжі для завершення дня 🍛",
            "Зробіть вечір трохи смачнішим 🍗",
        )
    }

    private fun weightTitles(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "A small check-in for today ⚖️",
            "See how you’re doing today 📊",
            "A quick moment for yourself ⚖️",
            "Stay in touch with your progress 🧭",
            "Just a gentle reminder for today ⚖️",
            "Keep an eye on your journey 📉",
            "A simple step towards your goal ⚖️",
        )
        AppLanguage.RUSSIAN -> listOf(
            "Небольшая проверка на сегодня ⚖️",
            "Посмотрите, как у вас сегодня дела 📊",
            "Короткий момент для себя ⚖️",
            "Оставайтесь на связи со своим прогрессом 🧭",
            "Нежное напоминание на сегодня ⚖️",
            "Следите за своим путем 📉",
            "Простой шаг к вашей цели ⚖️",
        )
        AppLanguage.UKRAINIAN -> listOf(
            "Невелика перевірка на сьогодні ⚖️",
            "Подивіться, як у вас сьогодні справи 📊",
            "Короткий момент для себе ⚖️",
            "Тримайте звʼязок зі своїм прогресом 🧭",
            "Легке нагадування на сьогодні ⚖️",
            "Слідкуйте за своїм шляхом 📉",
            "Простий крок до вашої цілі ⚖️",
        )
    }
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

private fun ReminderType.isMealType(): Boolean {
    return this == ReminderType.BREAKFAST || this == ReminderType.LUNCH || this == ReminderType.DINNER
}

private fun hasCompletedMealForToday(meals: List<MealEntry>, type: ReminderType): Boolean {
    val zone = ZoneId.systemDefault()
    val today = Instant.now().atZone(zone).toLocalDate()
    val mealKcalForPeriod = meals
        .asSequence()
        .filter { entry ->
            val mealTime = Instant.ofEpochMilli(entry.timestamp).atZone(zone)
            mealTime.toLocalDate() == today && mapHourToReminderType(mealTime.hour) == type
        }
        .sumOf { it.totalKcal.coerceAtLeast(0) }
    return mealKcalForPeriod >= FULL_MEAL_KCAL_THRESHOLD
}

private fun mapHourToReminderType(hour: Int): ReminderType? = when (hour) {
    in 5..10 -> ReminderType.BREAKFAST
    in 11..15 -> ReminderType.LUNCH
    in 16..21 -> ReminderType.DINNER
    else -> null
}
