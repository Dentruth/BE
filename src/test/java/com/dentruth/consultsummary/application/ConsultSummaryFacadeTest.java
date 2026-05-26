package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.dto.response.PresignedUrlResponse;
import com.dentruth.user.application.UserService;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
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

@ExtendWith(MockitoExtension.class)
class ConsultSummaryFacadeTest {

    @InjectMocks
    private ConsultSummaryFacade consultSummaryFacade;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private UserService userService;

    private static final UUID USER_ID = UUID.randomUUID();

    @DisplayName("ACTIVE 유저는 Presigned URL 발급에 성공한다.")
    @Test
    void shouldSucceedGetUploadUrl_whenUserIsActive() {
        //given
        String filename = "consultation.m4a";
        String contentType = "audio/mp4";

        User mockUser = mock(User.class);
        given(userService.findById(USER_ID, "Presigned URL 발급 요청")).willReturn(mockUser);
        doNothing().when(mockUser).validateStatus();

        PresignedUrlResponse mockResponse = PresignedUrlResponse.builder()
                .presignedUrl("https://s3.com/test")
                .s3Key("consultations/" + USER_ID + "//uuid.m4a")
                .expiresIn(300)
                .build();

        given(presignedUrlService.generateUploadUrl(filename, contentType, USER_ID)).willReturn(mockResponse);

        //when
        PresignedUrlResponse response = consultSummaryFacade.getUploadUrl(filename, contentType, USER_ID);

        //then
        assertThat(response.getPresignedUrl()).isEqualTo("https://s3.com/test");
        assertThat(response.getS3Key()).contains(USER_ID.toString());
        assertThat(response.getExpiresIn()).isEqualTo(300);
    }

    @DisplayName("SUSPENDED 유저는 Presigned URL 발급에 실패하고, 예외가 발생한다.")
    @Test
    void shouldFailGetUploadUrl_whenUserIsSuspended() {
        //given
        String filename = "consultation.m4a";
        String contentType = "audio/mp4";

        User mockUser = getUser(UserStatus.SUSPENDED);
        given(userService.findById(USER_ID, "Presigned URL 발급 요청")).willReturn(mockUser);

        //when, then
        assertThatThrownBy(() -> consultSummaryFacade.getUploadUrl(filename, contentType, USER_ID))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUSPENDED_USER.getMessage());
    }

    @DisplayName("BLOCKED 유저는 Presigned URL 발급에 실패하고, 예외가 발생한다.")
    @Test
    void shouldFailGetUploadUrl_whenUserIsBlocked() {
        //given
        String filename = "consultation.m4a";
        String contentType = "audio/mp4";

        User mockUser = getUser(UserStatus.BLOCKED);
        given(userService.findById(USER_ID, "Presigned URL 발급 요청")).willReturn(mockUser);

        //when, then
        assertThatThrownBy(() -> consultSummaryFacade.getUploadUrl(filename, contentType, USER_ID))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BLOCKED_USER.getMessage());
    }

    @DisplayName("WITHDRAWN, DELETED 유저는 Presigned URL 발급에 실패하고, 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태 : {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldThrowException_whenUserIsAlreadyWithdrawnOrDeletedDuringPresignedUrlGeneration(UserStatus userStatus) {
        //given
        String filename = "consultation.m4a";
        String contentType = "audio/mp4";

        User mockUser = getUser(userStatus);
        given(userService.findById(USER_ID, "Presigned URL 발급 요청")).willReturn(mockUser);

        //when, then
        assertThatThrownBy(() -> consultSummaryFacade.getUploadUrl(filename, contentType, USER_ID))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());
    }

    @DisplayName("존재하지 않는 유저는 Presigned URL 발급에 실패하고, 예외가 발생한다.")
    @Test
    void shouldFailGetUploadUrl_whenUserNotFound() {
        //given
        String filename = "consultation.m4a";
        String contentType = "audio/mp4";

        given(userService.findById(USER_ID, "Presigned URL 발급 요청"))
                .willThrow(new DentruthException(ErrorStatus.USER_NOT_FOUND));

        //when, then
        assertThatThrownBy(() -> consultSummaryFacade.getUploadUrl(filename, contentType, USER_ID))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());
    }

    private User getUser(UserStatus userStatus) {
        return User.builder()
                .id(USER_ID)
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
