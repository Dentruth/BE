package com.dentruth.schedule.domain;

import java.time.LocalDate;

public record WeekRange(
        LocalDate startDate,
        LocalDate endDate
) {

    public static WeekRange from(LocalDate date) {
        LocalDate startDate =
                date.minusDays(date.getDayOfWeek().getValue() % 7);

        return new WeekRange(
                startDate,
                startDate.plusDays(6)
        );
    }
}