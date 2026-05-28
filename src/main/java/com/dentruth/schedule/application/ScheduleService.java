package com.dentruth.schedule.application;

import com.dentruth.schedule.application.dto.request.CreateScheduleRequest;
import com.dentruth.schedule.application.dto.request.UpdateScheduleRequest;
import com.dentruth.schedule.application.dto.response.CreateScheduleResponse;
import com.dentruth.schedule.application.dto.response.ScheduleDetailResponse;
import com.dentruth.schedule.domain.entity.Schedule;
import com.dentruth.schedule.domain.repository.ScheduleRepository;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateScheduleResponse createSchedule(UUID userId, CreateScheduleRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Schedule schedule = Schedule.builder()
                .clinicName(request.getClinicName())
                .clinicPurpose(request.getClinicPurpose())
                .startDate(request.getStartDate())
                .startTime(request.getStartTime())
                .endDate(request.getEndDate())
                .endTime(request.getEndTime())
                .memo(request.getMemo())
                .user(user)
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);

        return CreateScheduleResponse.builder()
                .scheduleId(savedSchedule.getId())
                .build();
    }

    @Transactional
    public ScheduleDetailResponse updateSchedule(
            Long scheduleId,
            UpdateScheduleRequest request
    ) {

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        schedule.updateSchedule(
                request.getClinicName(),
                request.getClinicPurpose(),
                request.getStartDate(),
                request.getStartTime(),
                request.getEndDate(),
                request.getEndTime(),
                request.getMemo()
        );

        return ScheduleDetailResponse.from(schedule);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        scheduleRepository.delete(schedule);
    }

    public ScheduleDetailResponse getSchedule(Long scheduleId) {

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        return ScheduleDetailResponse.from(schedule);
    }
}
