package com.dentruth.schedule.domain.repository;

import com.dentruth.schedule.domain.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
}
