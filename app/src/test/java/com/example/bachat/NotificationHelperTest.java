package com.example.bachat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationHelperTest {

    @Test
    public void testPeakReminderMatchesUserSpecExactWording() {
        // Given Maharashtra / BESCOM baseline: 6 PM to 10 PM, +20% tariff, off-peak after 10 PM
        String peakHours = "6:00 PM – 10:00 PM";
        String surcharge = "+20% TARIFF (+₹1.10/u)";
        String offPeakHours = "10:00 PM – 6:00 AM";
        List<String> appliances = Arrays.asList("Washing Machine", "Geyser");
        double flexibleWatts = 2000.0;
        float tariffRate = 8.0f;

        String reminder = NotificationHelper.formatPeakReminder(
                peakHours, surcharge, offPeakHours, appliances, flexibleWatts, tariffRate
        );

        String expected = "Surge Shield: Evening peak hours begin at 6:00 PM in your area (+20% tariff). Consider running the washing machine or geyser after 10:00 PM to save ₹ 14 today.";
        assertEquals(expected, reminder);
    }

    @Test
    public void testWeeklySummaryMatchesUserSpecExactWording() {
        int estimatedSavings = 210;
        String summary = NotificationHelper.formatWeeklySummary(estimatedSavings);

        String expected = "Great job! Your optimized schedule saved an estimated ₹ 210 this week.";
        assertEquals(expected, summary);
    }

    @Test
    public void testPeakReminderFallbackWhenNoAppliancesRegistered() {
        String peakHours = "6:00 PM – 10:00 PM";
        String surcharge = "+20% TARIFF";
        String offPeakHours = "10:00 PM – 6:00 AM";
        List<String> appliances = Collections.emptyList();

        String reminder = NotificationHelper.formatPeakReminder(
                peakHours, surcharge, offPeakHours, appliances, 0, 8.0f
        );

        // When user has not registered any appliance yet, it falls back to the canonical example
        String expected = "Surge Shield: Evening peak hours begin at 6:00 PM in your area (+20% tariff). Consider running the washing machine or geyser after 10:00 PM to save ₹ 14 today.";
        assertEquals(expected, reminder);
    }

    @Test
    public void testPeakReminderWithTamilNaduSchedule() {
        // Tamil Nadu: 6 PM - 9 PM, +25% surcharge, off-peak 10 PM
        String peakHours = "6:00 PM – 9:00 PM (Evening)";
        String surcharge = "+25% TARIFF SURCHARGE";
        String offPeakHours = "10:00 PM – 5:00 AM";
        List<String> appliances = Collections.singletonList("Washing Machine");

        String reminder = NotificationHelper.formatPeakReminder(
                peakHours, surcharge, offPeakHours, appliances, 1000.0, 8.0f
        );

        assertTrue(reminder.contains("begin at 6:00 PM"));
        assertTrue(reminder.contains("(+25% tariff)"));
        assertTrue(reminder.contains("running the washing machine"));
        assertTrue(reminder.contains("after 10:00 PM"));
    }

    @Test
    public void testPeakReminderWithUttarPradeshSchedule() {
        // UP: 5 PM - 11 PM, +15% surcharge, off-peak 11 PM
        String peakHours = "5:00 PM – 11:00 PM";
        String surcharge = "+15% TARIFF SURCHARGE";
        String offPeakHours = "11:00 PM – 6:00 AM";
        List<String> appliances = Arrays.asList("Water Heater", "Water Pump");

        String reminder = NotificationHelper.formatPeakReminder(
                peakHours, surcharge, offPeakHours, appliances, 3000.0, 8.0f
        );

        assertTrue(reminder.contains("begin at 5:00 PM"));
        assertTrue(reminder.contains("(+15% tariff)"));
        assertTrue(reminder.contains("after 11:00 PM"));
    }

    @Test
    public void testZeroEmojisInNotificationStrings() {
        String peakHours = "6:00 PM – 10:00 PM";
        String surcharge = "+20% TARIFF (+₹1.10/u)";
        String offPeakHours = "10:00 PM – 6:00 AM";
        List<String> appliances = Arrays.asList("Washing Machine", "Geyser");

        String reminder = NotificationHelper.formatPeakReminder(
                peakHours, surcharge, offPeakHours, appliances, 2000.0, 8.0f
        );
        String summary = NotificationHelper.formatWeeklySummary(210);

        assertNoEmojis(reminder);
        assertNoEmojis(summary);
    }

    private void assertNoEmojis(String text) {
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            // Check Unicode blocks for typical emojis & symbols
            boolean isEmoji = (codePoint >= 0x1F300 && codePoint <= 0x1F9FF)
                    || (codePoint >= 0x2600 && codePoint <= 0x26FF)
                    || (codePoint >= 0x2700 && codePoint <= 0x27BF)
                    || (codePoint >= 0x1FA70 && codePoint <= 0x1FAFF);
            assertFalse("Found forbidden emoji code point 0x" + Integer.toHexString(codePoint) + " in: " + text, isEmoji);
        }
    }
}
