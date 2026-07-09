package com.dentruth.schedule.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.schedule.application.dto.response.*;
import com.dentruth.schedule.domain.WeekRange;
import com.dentruth.schedule.domain.entity.enums.ScheduleType;
import com.dentruth.schedule.presentation.dto.request.CreateScheduleRequest;
import com.dentruth.schedule.presentation.dto.request.UpdateScheduleRequest;
import com.dentruth.schedule.domain.entity.Schedule;
import com.dentruth.schedule.domain.repository.ScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    private Schedule findScheduleById(Long scheduleId, UUID userId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> {

                    log.warn(
                            "일정을 찾을 수 없습니다. UserId=[{}], ScheduleId=[{}]",
                            userId,
                            scheduleId
                    );

                    return new DentruthException(
                            ErrorStatus.SCHEDULE_NOT_FOUND
                    );
                });
    }

    @Transactional
    public CreateScheduleResponse createSchedule(UUID userId, CreateScheduleRequest request) {

        log.info(
                "일정 생성 요청. UserId=[{}], ScheduleName=[{}]",
                userId,
                request.getScheduleName()
        );

        Schedule schedule =
                Schedule.createCustomSchedule(userId, request);

        Schedule savedSchedule = scheduleRepository.save(schedule);

        return CreateScheduleResponse.builder()
                .scheduleId(savedSchedule.getId())
                .build();
    }

    @Transactional
    public ScheduleDetailResponse updateSchedule(
            Long scheduleId,
            UUID userId,
            UpdateScheduleRequest request
    ) {

        log.info(
                "일정 수정 요청. UserId=[{}], ScheduleId=[{}]",
                userId,
                scheduleId
        );

        Schedule schedule = findScheduleById(scheduleId, userId);

        schedule.updateSchedule(
                request.getClinicName(),
                request.getClinicPurpose(),
                request.getScheduleName(),
                request.getStartDate(),
                request.getStartTime(),
                request.getEndDate(),
                request.getEndTime(),
                request.getMemo(),
                request.getScheduleType()
        );

        return ScheduleDetailResponse.from(schedule);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, UUID userId) {

        log.info(
                "일정 삭제 요청. UserId=[{}], ScheduleId=[{}]",
                userId,
                scheduleId
        );

        Schedule schedule = findScheduleById(scheduleId, userId);

        scheduleRepository.delete(schedule);
    }

    public ScheduleDetailResponse getSchedule(Long scheduleId, UUID userId) {

        Schedule schedule = findScheduleById(scheduleId, userId);

        return ScheduleDetailResponse.from(schedule);
    }

    public List<HomeScheduleResponse> getWeeklySchedules(UUID userId) {

        LocalDate today = LocalDate.now();

        WeekRange weekRange = WeekRange.from(today);

        List<Schedule> schedules =
                scheduleRepository.findAllByUserIdAndStartDateBetween(
                        userId,
                        weekRange.startDate(),
                        weekRange.endDate()
                );

        return schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getStartDate))
                .values()
                .stream()
                .map(dailySchedules -> {

                    Schedule firstSchedule = dailySchedules.stream()
                            .min(Comparator.comparing(Schedule::getStartTime))
                            .orElseThrow();

                    return HomeScheduleResponse.builder()
                            .id(firstSchedule.getId())
                            .date(firstSchedule.getStartDate())
                            .scheduleName(firstSchedule.getScheduleName())
                            .extraCount(dailySchedules.size() - 1)
                            .build();
                })
                .sorted(Comparator.comparing(HomeScheduleResponse::getDate).reversed())
                .toList();
    }

    public MonthlyScheduleResponse getMonthlySchedules(
            UUID userId,
            int year,
            int month
    ) {

        LocalDate startDate = LocalDate.of(year, month, 1);

        LocalDate endDate =
                startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Schedule> schedules = scheduleRepository
                .findAllByUserIdAndStartDateBetween(
                        userId,
                        startDate,
                        endDate
                );

        List<LocalDate> scheduleDates = schedules.stream()
                .map(Schedule::getStartDate)
                .distinct()
                .sorted()
                .toList();

        List<MonthlyScheduleItemResponse> monthlySchedules =
                schedules.stream()
                        .collect(Collectors.groupingBy(Schedule::getStartDate))
                        .values()
                        .stream()
                        .map(dailySchedules -> {

                            Schedule firstSchedule =
                                    dailySchedules.stream()
                                            .min(Comparator.comparing(Schedule::getStartTime))
                                            .orElseThrow();

                            return MonthlyScheduleItemResponse.builder()
                                    .id(firstSchedule.getId())
                                    .date(firstSchedule.getStartDate())
                                    .scheduleType(firstSchedule.getScheduleType())
                                    .scheduleName(firstSchedule.getScheduleName())
                                    .extraCount(dailySchedules.size() - 1)
                                    .build();
                        })
                        .sorted(
                                Comparator.comparing(
                                        MonthlyScheduleItemResponse::getDate
                                )
                        )
                        .toList();

        return MonthlyScheduleResponse.builder()
                .scheduleDates(scheduleDates)
                .schedules(monthlySchedules)
                .build();
    }

    public DetailScheduleResponse getDetailSchedules(
            UUID userId,
            LocalDate date
    ) {

        List<Schedule> schedules =
                scheduleRepository.findAllByUserIdAndStartDate(
                        userId,
                        date
                );

        List<CustomScheduleItemResponse> customSchedules =
                schedules.stream()
                        .filter(schedule ->
                                schedule.getScheduleType() == ScheduleType.CUSTOM)
                        .sorted(Comparator.comparing(Schedule::getStartTime))
                        .map(schedule ->
                                CustomScheduleItemResponse.builder()
                                        .id(schedule.getId())
                                        .date(schedule.getStartDate())
                                        .time(schedule.getStartTime())
                                        .clinicName(schedule.getClinicName())
                                        .scheduleName(schedule.getScheduleName())
                                        .memo(schedule.getMemo())
                                        .build()
                        )
                        .toList();

        List<ConsultationScheduleItemResponse> consultationSchedules =
                schedules.stream()
                        .filter(schedule ->
                                schedule.getScheduleType() == ScheduleType.CONSULTATION)
                        .sorted(Comparator.comparing(Schedule::getStartTime))
                        .map(schedule ->
                                ConsultationScheduleItemResponse.builder()
                                        .id(schedule.getId())
                                        .date(schedule.getStartDate())
                                        .clinicName(schedule.getClinicName())
                                        .scheduleName(schedule.getScheduleName())
                                        .build()
                        )
                        .toList();

        return DetailScheduleResponse.builder()
                .custom(customSchedules)
                .consultation(consultationSchedules)
                .build();
    }
}
