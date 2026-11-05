package com.iitsm.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formatting helpers for templates, used as {@code ${@fmt.dateTime(x)}}. Times are stored in UTC and
 * shown in the configured time zone ({@code itsm.timezone}, default Asia/Kolkata).
 */
@Component("fmt")
public class ViewFormatter {

    private final DateTimeFormatter dateTime;
    private final DateTimeFormatter time;

    public ViewFormatter(@Value("${itsm.timezone:Asia/Kolkata}") String zone) {
        ZoneId zoneId = ZoneId.of(zone);
        this.dateTime = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(zoneId);
        this.time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zoneId);
    }

    public String dateTime(Instant instant) {
        return instant == null ? "-" : dateTime.format(instant);
    }

    public String time(Instant instant) {
        return instant == null ? "-" : time.format(instant);
    }

    /** "in 12 min", "3 h 5 min overdue", ... */
    public String until(Instant due, Instant now) {
        if (due == null || now == null) {
            return "-";
        }
        Duration d = Duration.between(now, due);
        String text = human(d.abs());
        return d.isNegative() ? text + " overdue" : "in " + text;
    }

    public boolean isOverdue(Instant due, Instant now) {
        return due != null && now != null && now.isAfter(due);
    }

    static String human(Duration d) {
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        if (days > 0) {
            return days + " d " + hours + " h";
        }
        if (hours > 0) {
            return hours + " h " + minutes + " min";
        }
        return Math.max(minutes, 0) + " min";
    }
}
