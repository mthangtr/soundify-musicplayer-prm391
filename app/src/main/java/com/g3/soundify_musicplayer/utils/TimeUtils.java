package com.g3.soundify_musicplayer.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    
    /**
     * Format timestamp to readable date string
     */
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    /**
     * Format timestamp to readable time string
     */
    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }
    
    /**
     * Format timestamp to readable datetime string
     */
    public static String formatDateTime(long timestamp) {
        return DATETIME_FORMAT.format(new Date(timestamp));
    }
    
    /**
     * Format duration in milliseconds to MM:SS format
     */
    public static String formatDuration(int durationMs) {
        if (durationMs <= 0) {
            return "0:00";
        }
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes);
        
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
    
    /**
     * Format duration in milliseconds to HH:MM:SS format for longer durations
     */
    public static String formatLongDuration(int durationMs) {
        if (durationMs <= 0) {
            return "0:00:00";
        }
        
        long hours = TimeUnit.MILLISECONDS.toHours(durationMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours);
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }
    
    /**
     * Get relative time string (e.g., "2 hours ago", "yesterday")
     */
    public static String getRelativeTimeString(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + (days == 1 ? " day ago" : " days ago");
        } else {
            return formatDate(timestamp);
        }
    }
    
    /**
     * Convert MM:SS string to milliseconds
     */
    public static int parseTimeToMs(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0;
        }
        
        try {
            String[] parts = timeString.split(":");
            if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return (minutes * 60 + seconds) * 1000;
            }
        } catch (NumberFormatException e) {
            // Return 0 if parsing fails
        }
        
        return 0;
    }
    
    /**
     * Check if timestamp is today
     */
    public static boolean isToday(long timestamp) {
        long now = System.currentTimeMillis();
        long dayStart = now - (now % TimeUnit.DAYS.toMillis(1));
        return timestamp >= dayStart;
    }
    
    /**
     * Check if timestamp is yesterday
     */
    public static boolean isYesterday(long timestamp) {
        long now = System.currentTimeMillis();
        long dayStart = now - (now % TimeUnit.DAYS.toMillis(1));
        long yesterdayStart = dayStart - TimeUnit.DAYS.toMillis(1);
        return timestamp >= yesterdayStart && timestamp < dayStart;
    }
} 