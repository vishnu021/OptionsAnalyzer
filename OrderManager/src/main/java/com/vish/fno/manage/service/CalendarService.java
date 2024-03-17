package com.vish.fno.manage.service;

import com.vish.fno.manage.config.HolidaysConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CalendarService {
    private final Set<LocalDate> holidayDates;
    @Getter
    private final List<String> holidays;

    public CalendarService(HolidaysConfig holidaysConfig) {
        this.holidays = holidaysConfig.getHolidays();
        this.holidayDates = this.holidays.stream()
                .map(this::parseHoliday)
                .collect(Collectors.toSet());
    }
    // Date to Date methods
    public Date getPreviousNonHolidayDate(Date date) {
        LocalDate localDate = convertToLocalDate(date);
        LocalDate previousDate = findNextNonHolidayDate(localDate, -1);
        return Date.from(previousDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public Date getNextNonHolidayDate(Date date) {
        LocalDate localDate = convertToLocalDate(date);
        LocalDate nextDate = findNextNonHolidayDate(localDate, 1);
        return Date.from(nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // String to String methods
    public String getPreviousNonHolidayDateString(String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        LocalDate previousDate = findNextNonHolidayDate(date, -1);
        return previousDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public String getNextNonHolidayDateString(String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        LocalDate nextDate = findNextNonHolidayDate(date, 1);
        return nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private LocalDate parseHoliday(String holidayString) {
        try {
            return LocalDate.parse(holidayString);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", holidayString, e);
            return null;
        }
    }

    private LocalDate findNextNonHolidayDate(final LocalDate date, final int increment) {
        LocalDate nextDate = date.plusDays(increment);
        while (isHolidayOrWeekend(nextDate)) {
            nextDate = nextDate.plusDays(1);
        }
        return nextDate;
    }

    private boolean isHolidayOrWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY ||
                holidayDates.contains(date);
    }

    private LocalDate convertToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}

