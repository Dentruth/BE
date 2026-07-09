package com.dentruth.schedule.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import com.dentruth.consultprepare.domain.repository.ConsultPrepareRepository;
import com.dentruth.schedule.application.dto.response.*;
import com.dentruth.schedule.domain.WeekRange;
import com.dentruth.schedule.domain.entity.enums.ScheduleType;
import com.dentruth.schedule.presentation.dto.request.CreateScheduleRequest;
import com.dentruth.schedule.presentation.dto.request.UpdateScheduleRequest;
import com.dentruth.schedule.domain.entity.Schedule;
import com.dentruth.schedule.domain.repository.ScheduleRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ConsultPrepareRepository consultPrepareRepository;

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

        List<CalendarItem> items =
                findCalendarItems(
                        userId,
                        weekRange.startDate(),
                        weekRange.endDate()
                );

        return groupByDate(items)
                .values()
                .stream()
                .map(dailyItems -> {

                    CalendarItem first = pickEarliest(dailyItems);

                    return HomeScheduleResponse.builder()
                            .id(first.getId())
                            .date(first.getDate())
                            .scheduleType(first.getScheduleType())
                            .scheduleName(first.getScheduleName())
                            .extraCount(dailyItems.size() - 1)
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

        List<CalendarItem> items =
                findCalendarItems(userId, startDate, endDate);

        List<LocalDate> scheduleDates = items.stream()
                .map(CalendarItem::getDate)
                .distinct()
                .sorted()
                .toList();

        List<MonthlyScheduleItemResponse> monthlySchedules =
                groupByDate(items)
                        .values()
                        .stream()
                        .map(dailyItems -> {

                            CalendarItem first = pickEarliest(dailyItems);

                            return MonthlyScheduleItemResponse.builder()
                                    .id(first.getId())
                                    .date(first.getDate())
                                    .scheduleType(first.getScheduleType())
                                    .scheduleName(first.getScheduleName())
                                    .extraCount(dailyItems.size() - 1)
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

        List<CalendarItem> items = findCalendarItems(userId, date, date);

        List<CustomScheduleItemResponse> customSchedules =
                items.stream()
                        .filter(item ->
                                item.getScheduleType() == ScheduleType.CUSTOM)
                        .sorted(byTimeNullsFirst())
                        .map(item ->
                                CustomScheduleItemResponse.builder()
                                        .id(item.getId())
                                        .date(item.getDate())
                                        .time(item.getTime())
                                        .clinicName(item.getClinicName())
                                        .scheduleName(item.getScheduleName())
                                        .memo(item.getMemo())
                                        .build()
                        )
                        .toList();

        List<ConsultationScheduleItemResponse> consultationSchedules =
                items.stream()
                        .filter(item ->
                                item.getScheduleType() == ScheduleType.CONSULTATION)
                        .sorted(byTimeNullsFirst())
                        .map(item ->
                                ConsultationScheduleItemResponse.builder()
                                        .id(item.getId())
                                        .date(item.getDate())
                                        .clinicName(item.getClinicName())
                                        .scheduleName(item.getScheduleName())
                                        .build()
                        )
                        .toList();

        return DetailScheduleResponse.builder()
                .custom(customSchedules)
                .consultation(consultationSchedules)
                .build();
    }

    private List<CalendarItem> findCalendarItems(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<CalendarItem> scheduleItems =
                scheduleRepository
                        .findAllByUserIdAndStartDateBetween(
                                userId,
                                startDate,
                                endDate
                        )
                        .stream()
                        .map(CalendarItem::from)
                        .toList();

        List<CalendarItem> consultItems =
                consultPrepareRepository
                        .findAllByUserIdAndDeletedAtIsNullAndAppointmentDateBetween(
                                userId,
                                startDate.atStartOfDay(),
                                endDate.atTime(LocalTime.MAX)
                        )
                        .stream()
                        .map(CalendarItem::from)
                        .toList();

        return Stream.concat(scheduleItems.stream(), consultItems.stream())
                .toList();
    }

    private Map<LocalDate, List<CalendarItem>> groupByDate(
            List<CalendarItem> items
    ) {

        return items.stream()
                .collect(Collectors.groupingBy(CalendarItem::getDate));
    }

    private CalendarItem pickEarliest(List<CalendarItem> dailyItems) {

        return dailyItems.stream()
                .min(byTimeNullsFirst())
                .orElseThrow();
    }

    private Comparator<CalendarItem> byTimeNullsFirst() {

        return Comparator.comparing(
                CalendarItem::getTime,
                Comparator.nullsFirst(Comparator.naturalOrder())
        );
    }

    /**
     * Schedule(개인 일정)와 ConsultPrepare(상담카드)를 캘린더 조회 시점에
     * 하나의 형태로 합치기 위한 내부 전용 표현. ConsultPrepare는 예약 시간이
     * 없어 time은 null일 수 있다.
     */
    @Getter
    @Builder
    private static class CalendarItem {

        private Long id;
        private LocalDate date;
        private LocalTime time;
        private ScheduleType scheduleType;
        private String scheduleName;
        private String clinicName;
        private String memo;

        static CalendarItem from(Schedule schedule) {

            return CalendarItem.builder()
                    .id(schedule.getId())
                    .date(schedule.getStartDate())
                    .time(schedule.getStartTime())
                    .scheduleType(schedule.getScheduleType())
                    .scheduleName(schedule.getScheduleName())
                    .clinicName(schedule.getClinicName())
                    .memo(schedule.getMemo())
                    .build();
        }

        static CalendarItem from(ConsultPrepare consultPrepare) {

            return CalendarItem.builder()
                    .id(consultPrepare.getId())
                    .date(consultPrepare.getAppointmentDate().toLocalDate())
                    .time(null)
                    .scheduleType(ScheduleType.CONSULTATION)
                    .scheduleName(consultPrepare.getTitle())
                    .clinicName(consultPrepare.getClinicName())
                    .memo(null)
                    .build();
        }
    }
}
