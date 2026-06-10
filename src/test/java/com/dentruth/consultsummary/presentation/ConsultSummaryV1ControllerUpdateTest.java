package com.dentruth.consultsummary.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.common.domain.enums.Language;
import com.dentruth.common.jwt.JwtProperties;
import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import com.dentruth.consultsummary.presentation.dto.request.UpdateSummaryRequest;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

class ConsultSummaryV1ControllerUpdateTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsultSummaryRepository consultSummaryRepository;

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

    @DisplayName("정상 요청이면 ai 요약 수정에 성공하고 200을 반환한다.")
    @Test
    void shouldUpdateSummarySuccessfully_whenValidRequestIsProvided() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        ConsultSummary consultSummary = ConsultSummary.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .audioLink("audioLink")
                .failReason("[WHISPER_ERR] OpenAI STT Connection Timeout")
                .status(SummaryStatus.COMPLETED)
                .clinicName("강남 치과의원")
                .title(null)
                .diagnosticResult("result")
                .isDeleted(false)
                .build();

        consultSummaryRepository.save(consultSummary);

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getValidUpdateRequest();

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + consultSummary.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."));

        ConsultSummary updated = consultSummaryRepository.findById(consultSummary.getId()).orElseThrow();
        assertThat(updated.getClinicName()).isEqualTo("강남 치과의원");
        assertThat(updated.getDiagnosis()).isEqualTo("치근단 치주염");
    }

    @DisplayName("존재하지 않는 요약 정보 수정 요청 시 404를 반환한다.")
    @Test
    void shouldReturn404_whenConsultSummaryDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID nonExistentSummaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getValidUpdateRequest();

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + nonExistentSummaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(ErrorStatus.SUMMARY_RECORD_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.SUMMARY_RECORD_NOT_FOUND.getMessage()));
    }

    @DisplayName("삭제된 요약 정보 수정 요청 시 404를 반환한다.")
    @Test
    void shouldReturn404_whenConsultSummaryIsAlreadyDeleted() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        ConsultSummary deletedSummary = ConsultSummary.create(userId, "test/audio.m4a", "강남 치과");
        ReflectionTestUtils.setField(deletedSummary, "isDeleted", true);
        consultSummaryRepository.save(deletedSummary);

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getValidUpdateRequest();

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + deletedSummary.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(ErrorStatus.SUMMARY_RECORD_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.SUMMARY_RECORD_NOT_FOUND.getMessage()));
    }

    @DisplayName("본인 소유가 아닌 요약 정보 수정 요청 시 403을 반환한다.")
    @Test
    void shouldReturn403_whenConsultSummaryIsNotOwnedByUser() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        ConsultSummary othersSummary = ConsultSummary.create(anotherUserId, "test/audio.m4a", "강남 치과");
        consultSummaryRepository.save(othersSummary);

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getValidUpdateRequest();

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + othersSummary.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(ErrorStatus.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.FORBIDDEN.getMessage()));
    }

    @DisplayName("토큰이 존재하지 않으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsMissingDuringUpdate() throws Exception {
        //given
        UUID summaryId = UUID.randomUUID();
        UpdateSummaryRequest request = getValidUpdateRequest();

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsExpiredDuringUpdate() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();

        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        UpdateSummaryRequest request = getValidUpdateRequest();

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("만료된 access token 입니다."));
    }

    @DisplayName("clinicName이 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenClinicNameIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithClinicName("병");

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.clinicName").value("Must be at least 2 characters"));
    }

    @DisplayName("clinicName이 100자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenClinicNameExceedsOneHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithClinicName("병".repeat(101));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.clinicName").value("Cannot exceed 100 characters"));
    }

    @DisplayName("diagnosis.summary가 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenDiagnosisSummaryIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithDiagnosis(
                UpdateSummaryRequest.Diagnosis.builder()
                        .summary("치")
                        .summaryEng("Apical Periodontitis")
                        .description("치아 뿌리 끝에 염증이 생긴 상태예요.")
                        .descriptionEng("Inflammation at the root tip.")
                        .build()
        );

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['diagnosis.summary']").value("Must be at least 2 characters"));
    }

    @DisplayName("diagnosis.summary가 60자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenDiagnosisSummaryExceedsSixtyCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithDiagnosis(
                UpdateSummaryRequest.Diagnosis.builder()
                        .summary("치".repeat(61))
                        .summaryEng("Apical Periodontitis")
                        .description("치아 뿌리 끝에 염증이 생긴 상태예요.")
                        .descriptionEng("Inflammation at the root tip.")
                        .build()
        );

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['diagnosis.summary']").value("Cannot exceed 60 characters"));
    }

    @DisplayName("diagnosis.summaryEng가 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenDiagnosisSummaryEngIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithDiagnosis(
                UpdateSummaryRequest.Diagnosis.builder()
                        .summary("치근단 치주염")
                        .summaryEng("A")
                        .description("치아 뿌리 끝에 염증이 생긴 상태예요.")
                        .descriptionEng("Inflammation at the root tip.")
                        .build()
        );

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['diagnosis.summaryEng']").value("Must be at least 2 characters"));
    }

    @DisplayName("diagnosis.summaryEng가 60자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenDiagnosisSummaryEngExceedsSixtyCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithDiagnosis(
                UpdateSummaryRequest.Diagnosis.builder()
                        .summary("치근단 치주염")
                        .summaryEng("A".repeat(61))
                        .description("치아 뿌리 끝에 염증이 생긴 상태예요.")
                        .descriptionEng("Inflammation at the root tip.")
                        .build()
        );

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['diagnosis.summaryEng']").value("Cannot exceed 60 characters"));
    }

    @DisplayName("diagnosis.description이 300자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenDiagnosisDescriptionExceedsThreeHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithDiagnosis(
                UpdateSummaryRequest.Diagnosis.builder()
                        .summary("치근단 치주염")
                        .summaryEng("Apical Periodontitis")
                        .description("치".repeat(301))
                        .descriptionEng("Inflammation at the root tip.")
                        .build()
        );

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['diagnosis.description']").value("Cannot exceed 300 characters"));
    }

    @DisplayName("treatmentPlan[].plan이 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentPlanPlanIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentPlan(List.of(
                UpdateSummaryRequest.TreatmentPlan.builder()
                        .step(1)
                        .plan("신")
                        .planEng("Root canal therapy")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentPlan[0].plan']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentPlan[].plan이 300자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentPlanPlanExceedsThreeHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentPlan(List.of(
                UpdateSummaryRequest.TreatmentPlan.builder()
                        .step(1)
                        .plan("신".repeat(301))
                        .planEng("Root canal therapy")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentPlan[0].plan']").value("Cannot exceed 300 characters"));
    }

    @DisplayName("treatmentPlan[].planEng가 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentPlanPlanEngIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentPlan(List.of(
                UpdateSummaryRequest.TreatmentPlan.builder()
                        .step(1)
                        .plan("신경치료")
                        .planEng("R")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentPlan[0].planEng']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentPlan[].planEng가 300자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentPlanPlanEngExceedsThreeHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentPlan(List.of(
                UpdateSummaryRequest.TreatmentPlan.builder()
                        .step(1)
                        .plan("신경치료")
                        .planEng("R".repeat(301))
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentPlan[0].planEng']").value("Cannot exceed 300 characters"));
    }

    @DisplayName("treatmentDelay[].title이 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentDelayTitleIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentDelay(List.of(
                UpdateSummaryRequest.TreatmentDelay.builder()
                        .title("위")
                        .titleEng("Risk")
                        .description("방치하면 주변 뼈까지 염증이 퍼져요.")
                        .descriptionEng("Can spread to bone.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentDelay[0].title']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentDelay[].title이 100자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentDelayTitleExceedsOneHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentDelay(List.of(
                UpdateSummaryRequest.TreatmentDelay.builder()
                        .title("위".repeat(101))
                        .titleEng("Risk")
                        .description("방치하면 주변 뼈까지 염증이 퍼져요.")
                        .descriptionEng("Can spread to bone.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentDelay[0].title']").value("Cannot exceed 100 characters"));
    }

    @DisplayName("treatmentDelay[].titleEng가 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentDelayTitleEngIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentDelay(List.of(
                UpdateSummaryRequest.TreatmentDelay.builder()
                        .title("위험")
                        .titleEng("R")
                        .description("방치하면 주변 뼈까지 염증이 퍼져요.")
                        .descriptionEng("Can spread to bone.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentDelay[0].titleEng']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentDelay[].titleEng가 100자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentDelayTitleEngExceedsOneHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentDelay(List.of(
                UpdateSummaryRequest.TreatmentDelay.builder()
                        .title("위험")
                        .titleEng("R".repeat(101))
                        .description("방치하면 주변 뼈까지 염증이 퍼져요.")
                        .descriptionEng("Can spread to bone.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentDelay[0].titleEng']").value("Cannot exceed 100 characters"));
    }

    @DisplayName("treatmentDelay[].description이 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentDelayDescriptionIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentDelay(List.of(
                UpdateSummaryRequest.TreatmentDelay.builder()
                        .title("위험")
                        .titleEng("Risk")
                        .description("방")
                        .descriptionEng("Can spread to bone.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentDelay[0].description']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentDelay[].description이 300자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentDelayDescriptionExceedsThreeHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentDelay(List.of(
                UpdateSummaryRequest.TreatmentDelay.builder()
                        .title("위험")
                        .titleEng("Risk")
                        .description("방".repeat(301))
                        .descriptionEng("Can spread to bone.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentDelay[0].description']").value("Cannot exceed 300 characters"));
    }

    @DisplayName("treatmentDelay[].descriptionEng가 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentDelayDescriptionEngIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentDelay(List.of(
                UpdateSummaryRequest.TreatmentDelay.builder()
                        .title("위험")
                        .titleEng("Risk")
                        .description("방치하면 주변 뼈까지 염증이 퍼져요.")
                        .descriptionEng("C")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentDelay[0].descriptionEng']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentDelay[].descriptionEng가 300자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentDelayDescriptionEngExceedsThreeHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentDelay(List.of(
                UpdateSummaryRequest.TreatmentDelay.builder()
                        .title("위험")
                        .titleEng("Risk")
                        .description("방치하면 주변 뼈까지 염증이 퍼져요.")
                        .descriptionEng("C".repeat(301))
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentDelay[0].descriptionEng']").value("Cannot exceed 300 characters"));
    }

    // ============ treatmentAfterCare 검증 ============

    @DisplayName("treatmentAfterCare[].title이 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentAfterCareTitleIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentAfterCare(List.of(
                UpdateSummaryRequest.TreatmentAfterCare.builder()
                        .title("주")
                        .titleEng("Caution")
                        .description("회복이 느려질 수 있어요.")
                        .descriptionEng("Slows recovery.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentAfterCare[0].title']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentAfterCare[].title이 100자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentAfterCareTitleExceedsOneHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentAfterCare(List.of(
                UpdateSummaryRequest.TreatmentAfterCare.builder()
                        .title("주".repeat(101))
                        .titleEng("Caution")
                        .description("회복이 느려질 수 있어요.")
                        .descriptionEng("Slows recovery.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentAfterCare[0].title']").value("Cannot exceed 100 characters"));
    }

    @DisplayName("treatmentAfterCare[].titleEng가 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentAfterCareTitleEngIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentAfterCare(List.of(
                UpdateSummaryRequest.TreatmentAfterCare.builder()
                        .title("주의사항")
                        .titleEng("C")
                        .description("회복이 느려질 수 있어요.")
                        .descriptionEng("Slows recovery.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentAfterCare[0].titleEng']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentAfterCare[].titleEng가 100자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentAfterCareTitleEngExceedsOneHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentAfterCare(List.of(
                UpdateSummaryRequest.TreatmentAfterCare.builder()
                        .title("주의사항")
                        .titleEng("C".repeat(101))
                        .description("회복이 느려질 수 있어요.")
                        .descriptionEng("Slows recovery.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentAfterCare[0].titleEng']").value("Cannot exceed 100 characters"));
    }

    @DisplayName("treatmentAfterCare[].description이 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentAfterCareDescriptionIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentAfterCare(List.of(
                UpdateSummaryRequest.TreatmentAfterCare.builder()
                        .title("주의사항")
                        .titleEng("Caution")
                        .description("회")
                        .descriptionEng("Slows recovery.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentAfterCare[0].description']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentAfterCare[].description이 300자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentAfterCareDescriptionExceedsThreeHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentAfterCare(List.of(
                UpdateSummaryRequest.TreatmentAfterCare.builder()
                        .title("주의사항")
                        .titleEng("Caution")
                        .description("회".repeat(301))
                        .descriptionEng("Slows recovery.")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentAfterCare[0].description']").value("Cannot exceed 300 characters"));
    }

    @DisplayName("treatmentAfterCare[].descriptionEng가 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentAfterCareDescriptionEngIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentAfterCare(List.of(
                UpdateSummaryRequest.TreatmentAfterCare.builder()
                        .title("주의사항")
                        .titleEng("Caution")
                        .description("회복이 느려질 수 있어요.")
                        .descriptionEng("S")
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentAfterCare[0].descriptionEng']").value("Must be at least 2 characters"));
    }

    @DisplayName("treatmentAfterCare[].descriptionEng가 300자 초과면 400을 반환한다.")
    @Test
    void shouldReturn400_whenTreatmentAfterCareDescriptionEngExceedsThreeHundredCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());
        UpdateSummaryRequest request = getRequestWithTreatmentAfterCare(List.of(
                UpdateSummaryRequest.TreatmentAfterCare.builder()
                        .title("주의사항")
                        .titleEng("Caution")
                        .description("회복이 느려질 수 있어요.")
                        .descriptionEng("S".repeat(301))
                        .build()
        ));

        //when
        mockMvc.perform(put("/api/v1/consult-summaries/" + summaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result['treatmentAfterCare[0].descriptionEng']").value("Cannot exceed 300 characters"));
    }

    private UpdateSummaryRequest getValidUpdateRequest() {
        return UpdateSummaryRequest.builder()
                .clinicName("강남 치과의원")
                .diagnosis(UpdateSummaryRequest.Diagnosis.builder()
                        .summary("치근단 치주염")
                        .summaryEng("Apical Periodontitis")
                        .description("치아 뿌리 끝에 염증이 생긴 상태예요.")
                        .descriptionEng("Inflammation at the root tip.")
                        .build())
                .treatmentPlan(List.of(
                        UpdateSummaryRequest.TreatmentPlan.builder()
                                .step(1)
                                .plan("신경치료")
                                .planEng("Root canal therapy")
                                .build()
                ))
                .treatmentDelay(List.of(
                        UpdateSummaryRequest.TreatmentDelay.builder()
                                .title("위험")
                                .titleEng("Risk")
                                .description("방치하면 주변 뼈까지 염증이 퍼져요.")
                                .descriptionEng("Can spread to bone.")
                                .build()
                ))
                .treatmentAfterCare(List.of(
                        UpdateSummaryRequest.TreatmentAfterCare.builder()
                                .title("주의사항")
                                .titleEng("Caution")
                                .description("회복이 느려질 수 있어요.")
                                .descriptionEng("Slows recovery.")
                                .build()
                ))
                .build();
    }

    private UpdateSummaryRequest getRequestWithClinicName(String clinicName) {
        UpdateSummaryRequest valid = getValidUpdateRequest();
        return UpdateSummaryRequest.builder()
                .clinicName(clinicName)
                .diagnosis(valid.getDiagnosis())
                .treatmentPlan(valid.getTreatmentPlan())
                .treatmentDelay(valid.getTreatmentDelay())
                .treatmentAfterCare(valid.getTreatmentAfterCare())
                .build();
    }

    private UpdateSummaryRequest getRequestWithDiagnosis(UpdateSummaryRequest.Diagnosis diagnosis) {
        UpdateSummaryRequest valid = getValidUpdateRequest();
        return UpdateSummaryRequest.builder()
                .clinicName(valid.getClinicName())
                .diagnosis(diagnosis)
                .treatmentPlan(valid.getTreatmentPlan())
                .treatmentDelay(valid.getTreatmentDelay())
                .treatmentAfterCare(valid.getTreatmentAfterCare())
                .build();
    }

    private UpdateSummaryRequest getRequestWithTreatmentPlan(List<UpdateSummaryRequest.TreatmentPlan> treatmentPlan) {
        UpdateSummaryRequest valid = getValidUpdateRequest();
        return UpdateSummaryRequest.builder()
                .clinicName(valid.getClinicName())
                .diagnosis(valid.getDiagnosis())
                .treatmentPlan(treatmentPlan)
                .treatmentDelay(valid.getTreatmentDelay())
                .treatmentAfterCare(valid.getTreatmentAfterCare())
                .build();
    }

    private UpdateSummaryRequest getRequestWithTreatmentDelay(List<UpdateSummaryRequest.TreatmentDelay> treatmentDelay) {
        UpdateSummaryRequest valid = getValidUpdateRequest();
        return UpdateSummaryRequest.builder()
                .clinicName(valid.getClinicName())
                .diagnosis(valid.getDiagnosis())
                .treatmentPlan(valid.getTreatmentPlan())
                .treatmentDelay(treatmentDelay)
                .treatmentAfterCare(valid.getTreatmentAfterCare())
                .build();
    }

    private UpdateSummaryRequest getRequestWithTreatmentAfterCare(List<UpdateSummaryRequest.TreatmentAfterCare> treatmentAfterCare) {
        UpdateSummaryRequest valid = getValidUpdateRequest();
        return UpdateSummaryRequest.builder()
                .clinicName(valid.getClinicName())
                .diagnosis(valid.getDiagnosis())
                .treatmentPlan(valid.getTreatmentPlan())
                .treatmentDelay(valid.getTreatmentDelay())
                .treatmentAfterCare(treatmentAfterCare)
                .build();
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
