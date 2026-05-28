package com.dentruth.consultsummary.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.common.domain.enums.Language;
import com.dentruth.common.jwt.JwtProperties;
import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.consultsummary.application.TranscriptionEventPublisher;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import com.dentruth.consultsummary.presentation.dto.reqeust.CreateConsultSummaryRequest;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

class ConsultSummaryV1ControllerCreateSummaryTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsultSummaryRepository consultSummaryRepository;

    @MockitoBean
    private TranscriptionEventPublisher transcriptionEventPublisher;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        consultSummaryRepository.deleteAllInBatch();
    }

    @DisplayName("상담 기록 생성에 성공한다.")
    @Test
    void shouldCreateConsultSummarySuccessfully_whenValidRequestIsProvided() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_202"))
                .andExpect(jsonPath("$.message").value("요청 성공."))
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode resultNode = objectMapper.readTree(jsonResponse).get("result");

        UUID resultId = UUID.fromString(resultNode.get("id").asText());
        ConsultSummary consultSummary = consultSummaryRepository.findById(resultId)
                .orElseThrow();

        assertThat(resultNode.get("clinicName").asText()).isEqualTo(consultSummary.getClinicName());
        assertThat(resultNode.get("status").asText()).isEqualTo(consultSummary.getStatus().name());

        verify(transcriptionEventPublisher, times(1)).publish(any(), any(), any(), any());
    }

    @DisplayName("SUSPENDED 유저는 상담 요약 기록 생성에 실패하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenUserIsSuspendedDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.SUSPENDED));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("일시 정지된 계정입니다."));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("BLOCKED 유저는 상담 요약 기록 생성에 실패하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenUserIsBlockedDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.BLOCKED));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("차단된 계정입니다."));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("WITHDRAWN, DELETED 유저는 상담 요약 기록 생성에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN","DELETED"})
    void shouldReturn404_whenUserIsAlreadyWithdrawnOrDeletedDuringConsultSummaryCreation(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, userStatus));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("유저가 존재하지 않으면 상담 요약 기록 생성에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExistDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("토큰이 존재하지 않으면 상담 요약 기록 생성에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsMissingDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isUnauthorized());

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("토큰이 만료되었으면 상담 요약 기록 생성에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsExpiredDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("강남 병원")
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("만료된 access token 입니다."));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("병원 이름이 null이면 상담 요약 기록 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenClinicNameIsNullDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.clinicName").value("병원 이름은 필수입니다."));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("병원 이름이 2자 미만이면 상담 요약 기록 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenClinicNameIsLessThanTwoCharactersDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("병")
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.clinicName").value("병원 이름은 2~100자 사이여야 합니다. "));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("병원 이름이 100자 초과면 상담 요약 기록 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenClinicNameExceedsOneHundredCharactersDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("병".repeat(101))
                .audioLink("test/asdfqwer/asdfqer.m4a")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.clinicName").value("병원 이름은 2~100자 사이여야 합니다. "));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("음성 파일이 null이면 상담 요약 기록 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenAudioLinkIsNullDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("강남 병원")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.audioLink").value("음성 파일은 필수입니다."));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @DisplayName("음성 파일이 공백이면 상담 요약 기록 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenAudioLinkIsBlankDuringConsultSummaryCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        CreateConsultSummaryRequest request = CreateConsultSummaryRequest.builder()
                .clinicName("강남 병원")
                .audioLink("  ")
                .build();

        //when
        mockMvc.perform(post("/api/v1/consult-summaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.audioLink").value("음성 파일은 필수입니다."));

        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
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
