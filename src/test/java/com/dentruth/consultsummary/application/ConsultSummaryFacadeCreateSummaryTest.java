package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dentruth.common.domain.enums.Language;
import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.dto.request.CreateConsultSummaryApplicationRequest;
import com.dentruth.consultsummary.application.dto.response.CreateConsultSummaryResponse;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.dentruth.user.application.UserService;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsultSummaryFacadeCreateSummaryTest {

    @InjectMocks
    private ConsultSummaryFacade consultSummaryFacade;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserService userService;

    @Mock
    private ConsultSummaryService consultSummaryService;

    @Mock
    private TranscriptionEventPublisher transcriptionEventPublisher;

    private final UUID userId = UUID.randomUUID();

    @DisplayName("상담 요약 기록 생성에 성공한다.")
    @Test
    void shouldCreateConsultSummarySuccessfully_whenValidRequestIsProvided() {
        //given
        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer1234")
                .build();

        User mockUser = mock(User.class);
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);

        ConsultSummary consultSummary = ConsultSummary.builder()
                .id(UUID.randomUUID())
                .clinicName(request.getClinicName())
                .audioLink(request.getAudioLink())
                .status(SummaryStatus.PENDING)
                .userId(userId)
                .isDeleted(false)
                .build();
        given(consultSummaryService.saveCreateConsultSummary(eq(userId), eq(request.getAudioLink()),
                eq(request.getClinicName()))).willReturn(consultSummary);
        ReflectionTestUtils.setField(consultSummary, "createdAt", Instant.now());

        //when
        CreateConsultSummaryResponse response = consultSummaryFacade.createConsultSummary(userId, request);

        //then
        assertThat(response.getId()).isEqualTo(consultSummary.getId());
        assertThat(response.getClinicName()).isEqualTo(consultSummary.getClinicName());
        assertThat(response.getStatus()).isEqualTo(consultSummary.getStatus());
        assertThat(response.getCreatedAt()).isNotNull();

        verify(transcriptionEventPublisher, times(1)).publish(any(), any(), any(), any());
    }

    @DisplayName("SUSPENDED 유저면 상담 요약 기록 생성에 실패하고, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIsSuspendedDuringConsultSummaryCreation() {
        //given
        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer1234")
                .build();

        User mockUser = getUser(UserStatus.SUSPENDED);
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);


        //when
        assertThatThrownBy(() -> consultSummaryFacade.createConsultSummary(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUSPENDED_USER.getMessage());

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("BLOCKED 유저면 상담 요약 기록 생성에 실패하고, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIsBlockedDuringConsultSummaryCreation() {
        //given
        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer1234")
                .build();

        User mockUser = getUser(UserStatus.BLOCKED);
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);


        //when
        assertThatThrownBy(() -> consultSummaryFacade.createConsultSummary(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BLOCKED_USER.getMessage());

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("WITHDRAWN, DELETED 유저면 상담 요약 기록 생성에 실패하고, 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @EnumSource(value = UserStatus.class, names = { "WITHDRAWN", "DELETED" })
    void shouldThrowException_whenUserIsAlreadyWithdrawnOrDeletedDuringConsultSummaryCreation(UserStatus userStatus) {
        //given
        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer1234")
                .build();

        User mockUser = getUser(userStatus);
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);

        //when
        assertThatThrownBy(() -> consultSummaryFacade.createConsultSummary(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    private User getUser(UserStatus userStatus) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
                .password("password1234!")
                .status(userStatus)
                .userType(UserType.LOCAL)
                .name("기존 이름")
                .language(Language.KOREAN)
                .birth(LocalDate.of(2002, 5, 21))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();
    }

}
