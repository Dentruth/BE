package com.dentruth.consultsummary.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class ConsultSummaryV1ControllerDeleteSummariesTest extends ControllerTestSupport {

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

    @DisplayName("유저가 존재하고, 삭제할 요약본이 존재하면 정상적으로 삭제된다.")
    @Test
    void shouldDeleteConsultSummarySuccessfully_whenUserAndSummaryExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        ConsultSummary consultSummary1 = getConsultSummary(userId);
        ConsultSummary consultSummary2 = getConsultSummary(userId);
        consultSummaryRepository.saveAll(List.of(consultSummary1, consultSummary2));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/consult-summaries")
                        .param("summaryIds", consultSummary1.getId().toString())
                        .param("summaryIds", consultSummary2.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        //then
        ConsultSummary after1 = consultSummaryRepository.findById(consultSummary1.getId()).orElseThrow();
        ConsultSummary after2 = consultSummaryRepository.findById(consultSummary2.getId()).orElseThrow();

        assertThat(after1.getIsDeleted()).isTrue();
        assertThat(after2.getIsDeleted()).isTrue();
    }

    @DisplayName("삭제할 요약본이 하나라도 존재하지 않으면 모두 삭제에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAnyOfConsultSummariesDoesNotExistDuringDelete() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        ConsultSummary consultSummary1 = getConsultSummary(userId);
        ConsultSummary consultSummary2 = getConsultSummary(userId);
        consultSummaryRepository.saveAll(List.of(consultSummary1, consultSummary2));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/consult-summaries")
                        .param("summaryIds", consultSummary1.getId().toString())
                        .param("summaryIds", consultSummary2.getId().toString())
                        .param("summaryIds", UUID.randomUUID().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("CON_001"))
                .andExpect(jsonPath("$.message").value("요약 기록 정보가 없습니다."));

        //then
        ConsultSummary after1 = consultSummaryRepository.findById(consultSummary1.getId()).orElseThrow();
        ConsultSummary after2 = consultSummaryRepository.findById(consultSummary2.getId()).orElseThrow();

        assertThat(after1.getIsDeleted()).isFalse();
        assertThat(after2.getIsDeleted()).isFalse();
    }

    @DisplayName("본인 요약본이 아닌 요약본을 삭제 요청하면 요약본 삭제에 실패하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenRequestingDeleteOnOthersConsultSummary() throws Exception {
        //given
        UUID anotherUser = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        ConsultSummary consultSummary1 = getConsultSummary(anotherUser);
        ConsultSummary consultSummary2 = getConsultSummary(userId);
        consultSummaryRepository.saveAll(List.of(consultSummary1, consultSummary2));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/consult-summaries")
                        .param("summaryIds", consultSummary1.getId().toString())
                        .param("summaryIds", consultSummary2.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_403"))
                .andExpect(jsonPath("$.message").value("접근이 불가능합니다."));

        //then
        ConsultSummary after1 = consultSummaryRepository.findById(consultSummary1.getId()).orElseThrow();
        ConsultSummary after2 = consultSummaryRepository.findById(consultSummary2.getId()).orElseThrow();

        assertThat(after1.getIsDeleted()).isFalse();
        assertThat(after2.getIsDeleted()).isFalse();
    }

    @DisplayName("유저가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExistDuringDelete() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        ConsultSummary consultSummary1 = getConsultSummary(userId);
        ConsultSummary consultSummary2 = getConsultSummary(userId);
        consultSummaryRepository.saveAll(List.of(consultSummary1, consultSummary2));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/consult-summaries")
                        .param("summaryIds", consultSummary1.getId().toString())
                        .param("summaryIds", consultSummary2.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));

        //then
        ConsultSummary after1 = consultSummaryRepository.findById(consultSummary1.getId()).orElseThrow();
        ConsultSummary after2 = consultSummaryRepository.findById(consultSummary2.getId()).orElseThrow();

        assertThat(after1.getIsDeleted()).isFalse();
        assertThat(after2.getIsDeleted()).isFalse();
    }

    @DisplayName("차단된 유저가 요청하면 403을 반환한다.")
    @Test
    void shouldReturn403_whenBlockedUserRequests() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.BLOCKED));

        ConsultSummary consultSummary1 = getConsultSummary(userId);
        ConsultSummary consultSummary2 = getConsultSummary(userId);
        consultSummaryRepository.saveAll(List.of(consultSummary1, consultSummary2));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/consult-summaries")
                        .param("summaryIds", consultSummary1.getId().toString())
                        .param("summaryIds", consultSummary2.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("차단된 계정입니다."));

        //then
        ConsultSummary after1 = consultSummaryRepository.findById(consultSummary1.getId()).orElseThrow();
        ConsultSummary after2 = consultSummaryRepository.findById(consultSummary2.getId()).orElseThrow();

        assertThat(after1.getIsDeleted()).isFalse();
        assertThat(after2.getIsDeleted()).isFalse();
    }

    @DisplayName("일시 정지된 유저가 요청하면 403을 반환한다.")
    @Test
    void shouldReturn403_whenSuspendedUserRequests() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.SUSPENDED));

        ConsultSummary consultSummary1 = getConsultSummary(userId);
        ConsultSummary consultSummary2 = getConsultSummary(userId);
        consultSummaryRepository.saveAll(List.of(consultSummary1, consultSummary2));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/consult-summaries")
                        .param("summaryIds", consultSummary1.getId().toString())
                        .param("summaryIds", consultSummary2.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("일시 정지된 계정입니다."));

        //then
        ConsultSummary after1 = consultSummaryRepository.findById(consultSummary1.getId()).orElseThrow();
        ConsultSummary after2 = consultSummaryRepository.findById(consultSummary2.getId()).orElseThrow();

        assertThat(after1.getIsDeleted()).isFalse();
        assertThat(after2.getIsDeleted()).isFalse();
    }

    @DisplayName("유저 상태가 WITHDRAWN, DELETED라면 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태 : {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404_whenUserIsAlreadyWithdrawnOrDeleted(UserStatus status) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, status));

        ConsultSummary consultSummary1 = getConsultSummary(userId);
        ConsultSummary consultSummary2 = getConsultSummary(userId);
        consultSummaryRepository.saveAll(List.of(consultSummary1, consultSummary2));

        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/consult-summaries")
                        .param("summaryIds", consultSummary1.getId().toString())
                        .param("summaryIds", consultSummary2.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));

        //then
        ConsultSummary after1 = consultSummaryRepository.findById(consultSummary1.getId()).orElseThrow();
        ConsultSummary after2 = consultSummaryRepository.findById(consultSummary2.getId()).orElseThrow();

        assertThat(after1.getIsDeleted()).isFalse();
        assertThat(after2.getIsDeleted()).isFalse();
    }

    private ConsultSummary getConsultSummary(UUID userId) {
        return ConsultSummary.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .audioLink("audioLink")
                .failReason(null)
                .status(SummaryStatus.COMPLETED)
                .clinicName("강남 치과의원")
                .title("치근단 치주염")
                .diagnosticResult(null)
                .isDeleted(false)
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
