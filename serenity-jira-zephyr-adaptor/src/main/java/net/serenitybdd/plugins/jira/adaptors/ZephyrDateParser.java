package net.serenitybdd.plugins.jira.adaptors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;

public class ZephyrDateParser {

    private final DateTime today;
    private final DateTime yesterday;
    private final DateTime sunday;
    private final DateTime wednesday;
    private final DateTime saturday;
    private final DateTime friday;
    private final DateTime thursday;




    public ZephyrDateParser(DateTime today) {
        this.today = today;
        this.yesterday = today.minusDays(1);
        this.sunday = today.withDayOfWeek(DateTimeConstants.SUNDAY);
        this.wednesday = today.withDayOfWeek(DateTimeConstants.WEDNESDAY);
        this.saturday = today.withDayOfWeek(DateTimeConstants.SATURDAY);
        this.friday = today.withDayOfWeek(DateTimeConstants.FRIDAY);
        this.thursday = today.withDayOfWeek(DateTimeConstants.THURSDAY);
    }

    public DateTime parse(String date) {

        if (date.contains("Today")) {
            date = date.replace("Today", today.toString(DateTimeFormat.forPattern("d/MMM/yy")));
        } else if (date.contains("Yesterday")) {
            date = date.replace("Yesterday", yesterday.toString(DateTimeFormat.forPattern("d/MMM/yy")));
        } else if (date.contains("Sunday")) {
            date = date.replace("Sunday", sunday.toString(DateTimeFormat.forPattern("d/MMM/yy")));
        } else if (date.contains("Wednesday")) {
            date = date.replace("Wednesday", wednesday.toString(DateTimeFormat.forPattern("d/MMM/yy")));
        } else if (date.contains("Saturday")) {
            date = date.replace("Saturday", saturday.toString(DateTimeFormat.forPattern("d/MMM/yy")));
        } else if (date.contains("Friday")) {
            date = date.replace("Friday", friday.toString(DateTimeFormat.forPattern("d/MMM/yy")));
        } else if (date.contains("Thursday")) {
            date = date.replace("Thursday", thursday.toString(DateTimeFormat.forPattern("d/MMM/yy")));
        }

        for(int daysBack = 2; daysBack < 7; daysBack++) {
            DateTime aPreviousDay = today.minusDays(daysBack);
            String day = aPreviousDay.dayOfWeek().getAsText();
            if (date.contains(day)) {
                date = date.replace(day, aPreviousDay.toString(DateTimeFormat.forPattern("d/MMM/yy")));
            }
        }

        return DateTime.parse(date, DateTimeFormat.forPattern("d/MMM/yy hh:mm a"));
    }
}
