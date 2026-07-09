package com.dentruth.schedule.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import com.dentruth.consultprepare.domain.entity.enums.CurrentStatus;
import com.dentruth.consultprepare.domain.entity.enums.DrinkingLevel;
import com.dentruth.consultprepare.domain.entity.enums.ExerciseLevel;
import com.dentruth.consultprepare.domain.entity.enums.SmokingLevel;
import com.dentruth.consultprepare.domain.repository.ConsultPrepareRepository;
import com.dentruth.schedule.application.dto.response.DetailScheduleResponse;
import com.dentruth.schedule.application.dto.response.HomeScheduleResponse;
import com.dentruth.schedule.application.dto.response.MonthlyScheduleResponse;
import com.dentruth.schedule.domain.WeekRange;
import com.dentruth.schedule.domain.entity.Schedule;
import com.dentruth.schedule.domain.entity.enums.ClinicPurpose;
import com.dentruth.schedule.domain.entity.enums.ScheduleType;
import com.dentruth.schedule.domain.repository.ScheduleRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @InjectMocks
    private ScheduleService scheduleService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ConsultPrepareRepository consultPrepareRepository;

    private static final UUID USER_ID = UUID.randomUUID();

    @DisplayName("주간 조회 시 개인 일정과 상담카드가 함께 노출된다.")
    @Test
    void shouldMergeConsultPrepareIntoWeeklySchedules() {
        //given
        LocalDate today = LocalDate.now();
        WeekRange weekRange = WeekRange.from(today);

        Schedule customSchedule = customSchedule(weekRange.startDate(), LocalTime.of(9, 0));
        ConsultPrepare consultPrepare = consultPrepare(weekRange.endDate());

        given(scheduleRepository.findAllByUserIdAndStartDateBetween(
                USER_ID, weekRange.startDate(), weekRange.endDate()))
                .willReturn(List.of(customSchedule));

        given(consultPrepareRepository.findAllByUserIdAndDeletedAtIsNullAndAppointmentDateBetween(
                any(), any(), any()))
                .willReturn(List.of(consultPrepare));

        //when
        List<HomeScheduleResponse> result = scheduleService.getWeeklySchedules(USER_ID);

        //then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(HomeScheduleResponse::getScheduleType)
                .containsExactlyInAnyOrder(ScheduleType.CUSTOM, ScheduleType.CONSULTATION);
    }

    @DisplayName("월간 조회 시 scheduleDates에 상담카드 날짜도 포함된다.")
    @Test
    void shouldIncludeConsultPrepareDateInMonthlySchedules() {
        //given
        LocalDate consultDate = LocalDate.of(2026, 8, 15);
        ConsultPrepare consultPrepare = consultPrepare(consultDate);

        given(scheduleRepository.findAllByUserIdAndStartDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(consultPrepareRepository.findAllByUserIdAndDeletedAtIsNullAndAppointmentDateBetween(
                any(), any(), any()))
                .willReturn(List.of(consultPrepare));

        //when
        MonthlyScheduleResponse result =
                scheduleService.getMonthlySchedules(USER_ID, 2026, 8);

        //then
        assertThat(result.getScheduleDates()).containsExactly(consultDate);
        assertThat(result.getSchedules()).hasSize(1);
        assertThat(result.getSchedules().get(0).getScheduleType())
                .isEqualTo(ScheduleType.CONSULTATION);
        assertThat(result.getSchedules().get(0).getScheduleName())
                .isEqualTo(consultPrepare.getTitle());
    }

    @DisplayName("일별 상세 조회 시 상담카드는 consultation 목록에 들어간다.")
    @Test
    void shouldPutConsultPrepareInConsultationBucket() {
        //given
        LocalDate date = LocalDate.of(2026, 8, 20);

        Schedule customSchedule = customSchedule(date, LocalTime.of(14, 0));
        ConsultPrepare consultPrepare = consultPrepare(date);

        given(scheduleRepository.findAllByUserIdAndStartDateBetween(USER_ID, date, date))
                .willReturn(List.of(customSchedule));
        given(consultPrepareRepository.findAllByUserIdAndDeletedAtIsNullAndAppointmentDateBetween(
                any(), any(), any()))
                .willReturn(List.of(consultPrepare));

        //when
        DetailScheduleResponse result = scheduleService.getDetailSchedules(USER_ID, date);

        //then
        assertThat(result.getCustom()).hasSize(1);
        assertThat(result.getCustom().get(0).getScheduleName()).isEqualTo("스케일링");

        assertThat(result.getConsultation()).hasSize(1);
        assertThat(result.getConsultation().get(0).getScheduleName())
                .isEqualTo(consultPrepare.getTitle());
        assertThat(result.getConsultation().get(0).getId()).isEqualTo(consultPrepare.getId());
    }

    private Schedule customSchedule(LocalDate date, LocalTime time) {
        return Schedule.builder()
                .id(1L)
                .clinicName("덴트루스치과")
                .clinicPurpose(ClinicPurpose.TREATMENT)
                .scheduleName("스케일링")
                .startDate(date)
                .startTime(time)
                .endDate(date)
                .endTime(time.plusHours(1))
                .scheduleType(ScheduleType.CUSTOM)
                .userId(USER_ID)
                .build();
    }

    private ConsultPrepare consultPrepare(LocalDate appointmentDate) {
        return ConsultPrepare.builder()
                .id(100L)
                .userId(USER_ID)
                .title("정기 검진 상담카드")
                .appointmentDate(LocalDateTime.of(appointmentDate, LocalTime.MIDNIGHT))
                .currentStatus(CurrentStatus.SHORT_STAY)
                .painExistence(false)
                .smoking(SmokingLevel.NON_SMOKER)
                .drinking(DrinkingLevel.NON_DRINK)
                .exercise(ExerciseLevel.REGULAR)
                .build();
    }

}
