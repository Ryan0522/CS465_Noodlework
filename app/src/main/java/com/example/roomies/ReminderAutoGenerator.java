package com.example.roomies;

import android.annotation.SuppressLint;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ReminderAutoGenerator {

    @SuppressLint("NewApi")
    public static long computeTriggerMillis(String dayLabel, String timeText) {
        LocalTime time = parseTime(timeText);
        LocalDate date = computeNextDate(dayLabel, time);
        ZonedDateTime zdt = date.atTime(time).atZone(ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    @SuppressLint("NewApi")
    private static LocalTime parseTime(String timeText) {
        // Normalize formats
        String t = timeText.toUpperCase().trim();

        // Try: 6 PM
        try {
            java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("h a");
            return LocalTime.parse(t, fmt);
        } catch (Exception ignored) {}

        // Try: 6:30 PM
        try {
            java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("h:mm a");
            return LocalTime.parse(t, fmt);
        } catch (Exception ignored) {}

        // Fallback
        return LocalTime.now();
    }

    @SuppressLint("NewApi")
    private static LocalDate computeNextDate(String dayLabel, LocalTime time) {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        dayLabel = dayLabel.trim().toLowerCase();

        // Today / Tomorrow special cases
        if (dayLabel.equals("today")) {
            if (time.isBefore(nowTime)) {
                return today.plusDays(1);
            }
            return today;
        }

        if (dayLabel.equals("tomorrow")) {
            return today.plusDays(1);
        }

        // Handle short names first: mon, tue...
        java.time.DayOfWeek target = mapShortDayToDow(dayLabel);
        if (target == null) {
            // Try full names as a fallback, e.g. "monday"
            try {
                target = java.time.DayOfWeek.valueOf(dayLabel.toUpperCase());
            } catch (Exception e) {
                // Bad label → make it tomorrow
                return today.plusDays(1);
            }
        }

        java.time.DayOfWeek todayDOW = today.getDayOfWeek();
        int diff = target.getValue() - todayDOW.getValue();
        if (diff < 0 || (diff == 0 && time.isBefore(nowTime))) {
            diff += 7;
        }
        return today.plusDays(diff);
    }

    @SuppressLint("NewApi")
    private static java.time.DayOfWeek mapShortDayToDow(String label) {
        String l = label.trim().toLowerCase();
        switch (l) {
            case "mon": return java.time.DayOfWeek.MONDAY;
            case "tue": return java.time.DayOfWeek.TUESDAY;
            case "wed": return java.time.DayOfWeek.WEDNESDAY;
            case "thu": return java.time.DayOfWeek.THURSDAY;
            case "fri": return java.time.DayOfWeek.FRIDAY;
            case "sat": return java.time.DayOfWeek.SATURDAY;
            case "sun": return java.time.DayOfWeek.SUNDAY;
            default: return null;
        }
    }

    private static final List<String> WEEK = Arrays.asList("Mon","Tue","Wed","Thu","Fri","Sat","Sun");

    private ReminderAutoGenerator() {}

    private static int idxOf(String day) {
        int i = WEEK.indexOf(day);
        return i >= 0 ? i : -1;
    }

    /** Find the closest auto-day on or before the dueDay; if none, return dueDay. */
    private static String nearestAutoDayOnOrBefore(String dueDay, List<String> autoDays) {
        int dueIdx = idxOf(dueDay);
        if (dueIdx == -1) return dueDay;

        int best = -1;
        for (String a : autoDays) {
            int ai = idxOf(a);
            if (ai == -1) continue;
            if (ai <= dueIdx && ai > best) {
                best = ai;
            }
        }
        return (best == -1) ? dueDay : WEEK.get(best);
    }

    /** Build auto reminders for one chore for THIS week labeling: "Day Time" strings. */
    static List<ReminderEntity> buildAutoRemindersForChore(
            ChoreEntity chore,
            List<String> autoDays,
            List<String> autoTimes
    ) {
        List<ReminderEntity> out = new ArrayList<>();
        if (chore == null) return out;
        if (autoDays == null || autoDays.isEmpty()) return out;
        if (autoTimes == null || autoTimes.isEmpty()) return out;

        // chore.dueDays is CSV like "Mon,Thu"
        if (chore.dueDays == null || chore.dueDays.trim().isEmpty()) return out;

        String[] due = chore.dueDays.split(",");
        for (String rawDd : due) {
            String dd = rawDd.trim();
            if (dd.isEmpty()) continue;

            String reminderDay = nearestAutoDayOnOrBefore(dd, autoDays);
            // Only use the FIRST selected time
            String firstTime = autoTimes.get(0).trim();
            if (firstTime.isEmpty()) return out;

            String text = reminderDay + " " + firstTime;

            ReminderEntity entity = new ReminderEntity(chore.id, text, true);
            entity.triggerAtMillis = ReminderAutoGenerator.computeTriggerMillis(reminderDay, firstTime);
            out.add(entity);
        }
        return out;
    }
}