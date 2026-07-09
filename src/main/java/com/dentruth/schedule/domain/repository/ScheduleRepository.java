package com.dentruth.schedule.domain.repository;

import com.dentruth.schedule.domain.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findAllByUserIdAndStartDateBetween(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Schedule> findAllByUserIdAndStartDate(
            UUID userId,
            LocalDate startDate
    );

}
