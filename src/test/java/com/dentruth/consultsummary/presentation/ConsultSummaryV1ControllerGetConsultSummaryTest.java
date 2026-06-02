package com.dentruth.consultsummary.presentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.common.domain.enums.Language;
import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class ConsultSummaryV1ControllerGetConsultSummaryTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsultSummaryRepository consultSummaryRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        consultSummaryRepository.deleteAllInBatch();
    }

    private final static String mockJson = """
            {
              "diagnosis": {
                "summary": "치근단 치주염",
                "summaryEng": "Apical Periodontitis",
                "description": "치아 뿌리 끝에 염증이 생긴 상태예요.",
                "descriptionEng": "The nerve inside the tooth is dead."
              },
              "treatmentPlan": [
                {
                  "step": 1,
                  "plan": "오른쪽 아래 첫 번째 큰 어금니(#46) 신경치료",
                  "planEng": "Root canal therapy on the lower right first molar (#46)"
                }
              ],
              "treatmentDelay": [
                {
                  "title": "잇몸 내 염증 확대",
                  "titleEng": "Expansion of infection within the gums",
                  "description": "방치하면 주변 뼈까지 염증이 퍼져요.",
                  "descriptionEng": "If left untreated, the infection can spread."
                }
              ],
              "treatmentAfterCare": [
                {
                  "title": "치료 중 음주·흡연",
                  "titleEng": "Drinking or smoking during treatment",
                  "description": "회복이 느려질 수 있어요.",
                  "descriptionEng": "It may slow down recovery."
                }
              ]
            }
            """;

    @DisplayName("유저가 존재하고, 요약 정보가 존재하며 요약 상태가 COMPLETED라면 올바른 요약 정보를 반환한다.")
    @Test
    void shouldReturnCorrectConsultSummary_whenUserAndSummaryExistWithCompletedStatus() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID consultSummaryId = UUID.randomUUID();

        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        ConsultSummary consultSummary = ConsultSummary.builder()
                .id(consultSummaryId)
                .userId(userId)
                .audioLink("audioLink")
                .failReason(null)
                .status(SummaryStatus.COMPLETED)
                .clinicName("강남 치과의원")
                .title("치근단 치주염")
                .diagnosticResult(mockJson)
                .isDeleted(false)
                .build();

        consultSummaryRepository.save(consultSummary);

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/consult-summaries/" + consultSummaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))

                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))

                .andExpect(jsonPath("$.result.consultationInfo.clinicName").value("강남 치과의원"))
                .andExpect(jsonPath("$.result.consultationInfo.date").value(LocalDate.now().toString()))

                .andExpect(jsonPath("$.result.diagnosis.summary").value("치근단 치주염"))
                .andExpect(jsonPath("$.result.diagnosis.summaryEng").value("Apical Periodontitis"))
                .andExpect(jsonPath("$.result.diagnosis.description").value("치아 뿌리 끝에 염증이 생긴 상태예요."))
                .andExpect(jsonPath("$.result.diagnosis.descriptionEng").value("The nerve inside the tooth is dead."))

                .andExpect(jsonPath("$.result.treatmentPlan[0].step").value(1))
                .andExpect(jsonPath("$.result.treatmentPlan[0].plan").value("오른쪽 아래 첫 번째 큰 어금니(#46) 신경치료"))
                .andExpect(jsonPath("$.result.treatmentPlan[0].planEng").value(
                        "Root canal therapy on the lower right first molar (#46)"))

                .andExpect(jsonPath("$.result.treatmentDelay[0].title").value("잇몸 내 염증 확대"))
                .andExpect(
                        jsonPath("$.result.treatmentDelay[0].titleEng").value("Expansion of infection within the gums"))
                .andExpect(jsonPath("$.result.treatmentDelay[0].description").value("방치하면 주변 뼈까지 염증이 퍼져요."))
                .andExpect(jsonPath("$.result.treatmentDelay[0].descriptionEng").value(
                        "If left untreated, the infection can spread."))

                .andExpect(jsonPath("$.result.treatmentAfterCare[0].title").value("치료 중 음주·흡연"))
                .andExpect(
                        jsonPath("$.result.treatmentAfterCare[0].titleEng").value(
                                "Drinking or smoking during treatment"))
                .andExpect(jsonPath("$.result.treatmentAfterCare[0].description").value("회복이 느려질 수 있어요."))
                .andExpect(
                        jsonPath("$.result.treatmentAfterCare[0].descriptionEng").value("It may slow down recovery."))

                .andExpect(jsonPath("$.result.failReason").isEmpty())
                .andExpect(jsonPath("$.result.status").value("COMPLETED"));
    }

    @DisplayName("AI 요약 상태가 COMPLETED가 아니라면, AI 결과물은 매핑하지 않고 최소 기본 정보와 실패 사유만 반환한다.")
    @ParameterizedTest(name = "[{index}] 요약 상태 : {0}")
    @EnumSource(value = SummaryStatus.class, names = {"UPLOADING", "PENDING", "ANALYZING", "FAILED", "RETRYING"})
    void shouldReturnMinimalInfoAndFailReasonWithoutAiResult_whenStatusIsNotCompleted(SummaryStatus status)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID consultSummaryId = UUID.randomUUID();

        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        ConsultSummary consultSummary = ConsultSummary.builder()
                .id(consultSummaryId)
                .userId(userId)
                .audioLink("audioLink")
                .failReason("[WHISPER_ERR] OpenAI STT Connection Timeout")
                .status(status)
                .clinicName("강남 치과의원")
                .title(null)
                .diagnosticResult(mockJson)
                .isDeleted(false)
                .build();

        consultSummaryRepository.save(consultSummary);

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/consult-summaries/" + consultSummaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))

                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))

                .andExpect(jsonPath("$.result.consultationInfo.clinicName").value("강남 치과의원"))
                .andExpect(jsonPath("$.result.consultationInfo.date").value(LocalDate.now().toString()))

                .andExpect(jsonPath("$.result.diagnosis").isEmpty())
                .andExpect(jsonPath("$.result.treatmentPlan").isEmpty())
                .andExpect(jsonPath("$.result.treatmentDelay").isEmpty())
                .andExpect(jsonPath("$.result.treatmentAfterCare").isEmpty())

                .andExpect(jsonPath("$.result.failReason").value("[WHISPER_ERR] OpenAI STT Connection Timeout"))
                .andExpect(jsonPath("$.result.status").value(status.name()));
    }

    @DisplayName("요약 기록 정보가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenConsultSummaryDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID consultSummaryId = UUID.randomUUID();

        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when, then
        mockMvc.perform(get("/api/v1/consult-summaries/" + consultSummaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("CON_001"))
                .andExpect(jsonPath("$.message").value("요약 기록 정보가 없습니다."));
    }

    @DisplayName("유저 상태가 WITHDRAWN, DELETED라면 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태 : {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404_whenUserIsAlreadyWithdrawnOrDeleted(UserStatus status) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID consultSummaryId = UUID.randomUUID();

        userRepository.save(getUser(userId, status));

        ConsultSummary consultSummary = ConsultSummary.builder()
                .id(consultSummaryId)
                .userId(userId)
                .audioLink("audioLink")
                .failReason("[WHISPER_ERR] OpenAI STT Connection Timeout")
                .status(SummaryStatus.COMPLETED)
                .clinicName("강남 치과의원")
                .title(null)
                .diagnosticResult(mockJson)
                .isDeleted(false)
                .build();

        consultSummaryRepository.save(consultSummary);

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/consult-summaries/" + consultSummaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))

                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));
    }

    @DisplayName("유저가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID consultSummaryId = UUID.randomUUID();

        ConsultSummary consultSummary = ConsultSummary.builder()
                .id(consultSummaryId)
                .userId(userId)
                .audioLink("audioLink")
                .failReason("[WHISPER_ERR] OpenAI STT Connection Timeout")
                .status(SummaryStatus.COMPLETED)
                .clinicName("강남 치과의원")
                .title(null)
                .diagnosticResult(mockJson)
                .isDeleted(false)
                .build();

        consultSummaryRepository.save(consultSummary);

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/consult-summaries/" + consultSummaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))

                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));
    }

    @DisplayName("차단된 유저가 요청하면 403을 반환한다.")
    @Test
    void shouldReturn403_whenBlockedUserRequests() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID consultSummaryId = UUID.randomUUID();

        userRepository.save(getUser(userId, UserStatus.BLOCKED));

        ConsultSummary consultSummary = ConsultSummary.builder()
                .id(consultSummaryId)
                .userId(userId)
                .audioLink("audioLink")
                .failReason("[WHISPER_ERR] OpenAI STT Connection Timeout")
                .status(SummaryStatus.COMPLETED)
                .clinicName("강남 치과의원")
                .title(null)
                .diagnosticResult(mockJson)
                .isDeleted(false)
                .build();

        consultSummaryRepository.save(consultSummary);

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/consult-summaries/" + consultSummaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))

                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("차단된 계정입니다."));
    }

    @DisplayName("일시 정지된 유저가 요청하면 403을 반환한다.")
    @Test
    void shouldReturn403_whenSuspendedUserRequests() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID consultSummaryId = UUID.randomUUID();

        userRepository.save(getUser(userId, UserStatus.SUSPENDED));

        ConsultSummary consultSummary = ConsultSummary.builder()
                .id(consultSummaryId)
                .userId(userId)
                .audioLink("audioLink")
                .failReason("[WHISPER_ERR] OpenAI STT Connection Timeout")
                .status(SummaryStatus.COMPLETED)
                .clinicName("강남 치과의원")
                .title(null)
                .diagnosticResult(mockJson)
                .isDeleted(false)
                .build();

        consultSummaryRepository.save(consultSummary);

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/consult-summaries/" + consultSummaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))

                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("일시 정지된 계정입니다."));
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
