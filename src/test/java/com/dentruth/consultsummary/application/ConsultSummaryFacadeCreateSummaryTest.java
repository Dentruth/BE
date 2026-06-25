package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

    @DisplayName("본인 prefix의 S3 키로 요청하면 상담 요약이 정상적으로 생성된다.")
    @Test
    void shouldCreateConsultSummary_whenAudioLinkBelongsToRequester() {
        //given
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);

        String ownAudioLink = "consultations/" + userId + "/" + UUID.randomUUID() + ".m4a";
        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 치과의원")
                .audioLink(ownAudioLink)
                .build();

        ConsultSummary savedSummary = ConsultSummary.builder()
                .id(UUID.randomUUID())
                .clinicName("강남 치과의원")
                .status(SummaryStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(savedSummary, "createdAt", Instant.now());

        given(consultSummaryService.saveCreateConsultSummary(userId, ownAudioLink, "강남 치과의원"))
                .willReturn(savedSummary);

        //when
        CreateConsultSummaryResponse response = consultSummaryFacade.createConsultSummary(userId, request);

        //then
        assertThat(response.getId()).isEqualTo(savedSummary.getId());
        verify(transcriptionEventPublisher, times(1))
                .publish(eq(savedSummary.getId()), eq(ownAudioLink), eq("강남 치과의원"), any());
    }

    @DisplayName("본인 소유가 아닌 prefix의 S3 키로 요청하면 예외가 발생하고, 저장/이벤트 발행은 일어나지 않는다.")
    @Test
    void shouldThrowException_whenAudioLinkBelongsToAnotherUser() {
        //given
        UUID userId = UUID.randomUUID();
        UUID victimUserId = UUID.randomUUID();

        User mockUser = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);

        String othersAudioLink = "consultations/" + victimUserId + "/" + UUID.randomUUID() + ".m4a";
        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 치과의원")
                .audioLink(othersAudioLink)
                .build();

        //when, then
        assertThatThrownBy(() -> consultSummaryFacade.createConsultSummary(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.FORBIDDEN.getMessage());

        verify(consultSummaryService, never()).saveCreateConsultSummary(any(), any(), any());
        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("consultations/ 형식이 아닌 임의의 문자열을 audioLink로 보내면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenAudioLinkIsArbitraryString() {
        //given
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);

        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 치과의원")
                .audioLink("https://evil.com/payload.mp3")
                .build();

        //when, then
        assertThatThrownBy(() -> consultSummaryFacade.createConsultSummary(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.FORBIDDEN.getMessage());

        verify(consultSummaryService, never()).saveCreateConsultSummary(any(), any(), any());
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

    @DisplayName("V2 - 본인 prefix의 S3 키로 요청하면 상담 요약이 정상적으로 생성된다.")
    @Test
    void shouldCreateConsultSummaryV2_whenAudioLinkBelongsToRequester() {
        //given
        User mockUser = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);

        String ownAudioLink = "consultations/" + userId + "/" + UUID.randomUUID() + ".m4a";
        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 치과의원")
                .audioLink(ownAudioLink)
                .practitionerName("김의사")
                .licenseType("치과의사")
                .licenseNumber("12345")
                .institution("강남 치과의원")
                .build();

        ConsultSummary savedSummary = ConsultSummary.builder()
                .id(UUID.randomUUID())
                .clinicName("강남 치과의원")
                .status(SummaryStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(savedSummary, "createdAt", Instant.now());

        given(consultSummaryService.saveCreateConsultSummary(userId, request)).willReturn(savedSummary);

        //when
        CreateConsultSummaryResponse response = consultSummaryFacade.createConsultSummaryV2(userId, request);

        //then
        assertThat(response.getId()).isEqualTo(savedSummary.getId());
        verify(transcriptionEventPublisher, times(1))
                .publish(eq(savedSummary.getId()), eq(ownAudioLink), eq("강남 치과의원"), any());
    }

    @DisplayName("V2 - 본인 소유가 아닌 prefix의 S3 키로 요청하면 예외가 발생하고, 저장/이벤트 발행은 일어나지 않는다.")
    @Test
    void shouldThrowException_whenV2AudioLinkBelongsToAnotherUser() {
        //given
        UUID victimUserId = UUID.randomUUID();
        User mockUser = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);

        String othersAudioLink = "consultations/" + victimUserId + "/" + UUID.randomUUID() + ".m4a";
        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 치과의원")
                .audioLink(othersAudioLink)
                .practitionerName("김의사")
                .licenseType("치과의사")
                .build();

        //when, then
        assertThatThrownBy(() -> consultSummaryFacade.createConsultSummaryV2(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.FORBIDDEN.getMessage());

        verify(consultSummaryService, never()).saveCreateConsultSummary(any(), any(CreateConsultSummaryApplicationRequest.class));
        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("audioLink가 null이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenAudioLinkIsNull() {
        //given
        User mockUser = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);

        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 치과의원")
                .audioLink(null)
                .build();

        //when, then
        assertThatThrownBy(() -> consultSummaryFacade.createConsultSummary(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.FORBIDDEN.getMessage());
    }

    @DisplayName("GUEST 상태인 유저도 상담 요약 기록을 생성할 수 있다.")
    @Test
    void shouldCreateConsultSummary_whenUserStatusIsGuest() {
        //given
        User mockUser = User.builder().id(userId).status(UserStatus.GUEST).build();
        given(userService.findById(userId, "상담 요약 기록 생성")).willReturn(mockUser);

        String ownAudioLink = "consultations/" + userId + "/" + UUID.randomUUID() + ".m4a";
        CreateConsultSummaryApplicationRequest request = CreateConsultSummaryApplicationRequest.builder()
                .clinicName("강남 치과의원")
                .audioLink(ownAudioLink)
                .build();

        ConsultSummary savedSummary = ConsultSummary.builder()
                .id(UUID.randomUUID())
                .clinicName("강남 치과의원")
                .status(SummaryStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(savedSummary, "createdAt", Instant.now());

        given(consultSummaryService.saveCreateConsultSummary(userId, ownAudioLink, "강남 치과의원"))
                .willReturn(savedSummary);

        //when, then
        CreateConsultSummaryResponse response = consultSummaryFacade.createConsultSummary(userId, request);
        assertThat(response.getId()).isEqualTo(savedSummary.getId());
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
