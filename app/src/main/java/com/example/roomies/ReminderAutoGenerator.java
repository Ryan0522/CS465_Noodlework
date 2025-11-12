package com.example.roomies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ReminderAutoGenerator {

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
            out.add(new ReminderEntity(chore.id, text, true));
        }
        return out;
    }
}