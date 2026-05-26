package com.dentruth.consultsummary.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import com.google.common.net.HttpHeaders;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class ConsultSummaryV1ControllerUploadUrlTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("인증된 유저는 Presigned URL 발급에 성공한다.")
    @Test
    void shouldSucceedGetUploadUrl_whenAuthenticated() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        //when, then
        mockMvc.perform(get("/api/v1/consult-summaries/upload-url")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("filename", "consultation.m4a")
                        .param("contentType", "audio/mp4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.result.presignedUrl").isNotEmpty())
                .andExpect(jsonPath("$.result.s3Key").isNotEmpty())
                .andExpect(jsonPath("$.result.expiresIn").value(300));
    }

    @DisplayName("인증되지 않은 유저는 Presigned URL 발급에 실패한다.")
    @Test
    void shouldFailGetUploadUrl_whenNotAuthenticated() throws Exception {
        //when, then
        mockMvc.perform(get("/api/v1/consult-summaries/upload-url")
                        .param("filename", "consultation.m4a")
                        .param("contentType", "audio/mp4"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("존재하지 않는 유저는 Presigned URL 발급에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404NotFound_whenUserDoesNotExistDuringPresignedUrlGeneration() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when, then
        mockMvc.perform(get("/api/v1/consult-summaries/upload-url")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("filename", "consultation.m4a")
                        .param("contentType", "audio/mp4"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));
    }

    @DisplayName("SUSPENDED 유저는 Presigned URL 발급에 실패하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenUserIsSuspendedDuringPresignedUrlGeneration() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        userRepository.save(getUser(userId, UserStatus.SUSPENDED));

        //when, then
        mockMvc.perform(get("/api/v1/consult-summaries/upload-url")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("filename", "consultation.m4a")
                        .param("contentType", "audio/mp4"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("일시 정지된 계정입니다."));
    }

    @DisplayName("BLOCKED 유저는 Presigned URL 발급에 실패하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenUserIsBlockedDuringPresignedUrlGeneration() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        userRepository.save(getUser(userId, UserStatus.BLOCKED));

        //when, then
        mockMvc.perform(get("/api/v1/consult-summaries/upload-url")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("filename", "consultation.m4a")
                        .param("contentType", "audio/mp4"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("차단된 계정입니다."));
    }

    @DisplayName("WITHDRAWN, DELETED 유저는 Presigned URL 발급에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404NotFound_whenUserIsAlreadyWithdrawnOrDeletedDuringPresignedUrlGeneration(UserStatus userStatus)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        userRepository.save(getUser(userId, userStatus));

        //when, then
        mockMvc.perform(get("/api/v1/consult-summaries/upload-url")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("filename", "consultation.m4a")
                        .param("contentType", "audio/mp4"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));
    }

    @DisplayName("지원하지 않는 contentType이면 Presigned URL 발급에 실패한다.")
    @Test
    void shouldFailGetUploadUrl_whenInvalidContentType() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        //when, then
        mockMvc.perform(get("/api/v1/consult-summaries/upload-url")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("filename", "consultation.txt")
                        .param("contentType", "text/plain"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("CON_004"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 파일 형식입니다."));
    }

    private User getUser(UUID userId, UserStatus userStatus) {
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
