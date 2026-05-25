package com.dentruth.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.common.jwt.JwtProperties;
import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import com.dentruth.user.presentation.dto.request.UpdateUserInfoRequest;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class UserV1ControllerUpdateUserInfoTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("유저 정보 업데이트에 성공한다.")
    @Test
    void shouldSucceedUpdateUserInfo_whenRequestIsValid() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = getUpdateUserInfoRequest();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        MvcResult mvcResult = mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value("true"))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andReturn();

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode resultNode = objectMapper.readTree(jsonResponse).get("result");

        assertThat(resultNode.get("name").asText()).isEqualTo(updatedUser.getName());
        assertThat(resultNode.get("name").asText()).isNotEqualTo(user.getName());
        assertThat(updatedUser.getName()).isNotEqualTo(user.getName());

        assertThat(resultNode.get("language").asText()).isEqualTo(updatedUser.getLanguage().name());
        assertThat(resultNode.get("language").asText()).isNotEqualTo(user.getLanguage().name());
        assertThat(updatedUser.getLanguage().name()).isNotEqualTo(user.getLanguage().name());

        assertThat(resultNode.get("birth").asText()).isEqualTo(updatedUser.getBirth().toString());
        assertThat(resultNode.get("gender").asText()).isEqualTo(updatedUser.getGender().getKo());
        assertThat(resultNode.get("region").asText()).isEqualTo(updatedUser.getRegion());
        assertThat(resultNode.get("stayDuration").asText()).isEqualTo(updatedUser.getStayDuration().getKo());
        assertThat(resultNode.get("insuranceStatus").asText()).isEqualTo(updatedUser.getInsuranceStatus().getKo());
        assertThat(resultNode.get("nationality").asText()).isEqualTo(updatedUser.getNationality());
    }

    @DisplayName("유저가 WITHDRAWN, DELETED 상태면 404를 반환하고, 유저 정보 업데이트에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404NotFound_whenUserIsWithdrawnOrDeletedDuringUpdate(UserStatus status) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, status);
        userRepository.save(user);
        UpdateUserInfoRequest request = getUpdateUserInfoRequest();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("유저가 SUSPENDED 상태면 403을 반환하고, 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn403Forbidden_whenUserIsSuspendedDuringUpdate() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.SUSPENDED);
        userRepository.save(user);
        UpdateUserInfoRequest request = getUpdateUserInfoRequest();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("일시 정지된 계정입니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("유저가 BLOCKED 상태면 403을 반환하고, 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn403Forbidden_whenUserIsBlockedDuringUpdate() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.BLOCKED);
        userRepository.save(user);
        UpdateUserInfoRequest request = getUpdateUserInfoRequest();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("차단된 계정입니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("유저가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404NotFound_whenUserDoesNotExistDuringUpdate() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UpdateUserInfoRequest request = getUpdateUserInfoRequest();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));
    }

    @DisplayName("업데이트할 유저 이름이 null이면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenUpdateNameIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 24))
                .gender("F")
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.name").value("이름은 필수 입력입니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("업데이트할 유저 이름이 2자 미만이면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenUpdateNameIsLessThanTwoCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이")
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 24))
                .gender("F")
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.name").value("이름은 2~50자 사이여야 합니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("업데이트할 유저 이름이 50자 초과면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenUpdateNameExceedsFiftyCharacters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이".repeat(51))
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 24))
                .gender("F")
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.name").value("이름은 2~50자 사이여야 합니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("생년월일이 null이면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenBirthIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .language("KOREAN")
                .gender("F")
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.birthDate").value("생년월일은 필수입니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("생년월일이 미래 날짜면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenBirthIsInFuture() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .language("KOREAN")
                .birthDate(LocalDate.now().plusDays(1))
                .gender("F")
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("생년월일이 150년 이상의 과거라면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenBirthIsMoreThan150YearsAgo() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .language("KOREAN")
                .birthDate(LocalDate.now().minusYears(150).minusDays(1))
                .gender("F")
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("거주 지역이 null이면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenRegionIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender("F")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.region").value("거주지역은 필수입니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("선택된 언어가 null이면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenLanguageIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .region("서울 강남구")
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender("F")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.language").value("언어 선택은 필수입니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("선택된 언어 형식이 올바르지 않으면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenLanguageFormatIsInvalid() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .region("서울 강남구")
                .language("JAPANESE")
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender("F")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.language").value("유효한 언어 선택이 아닙니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("성별이 null이면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenGenderIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .region("서울 강남구")
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 21))
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.gender").value("성별은 필수입니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("유효한 성별이 아니면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenGenderIsInvalid() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .region("서울 강남구")
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender("X")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.gender").value("유효한 성별이 아닙니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("체류 기간이 null이면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenStayDurationIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .region("서울 강남구")
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender("F")
                .nationality("미국")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.stayDuration").value("체류기간은 필수입니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("체류 기간이 형식이 올바르지 않으면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenStayDurationFormatIsInvalid() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .region("서울 강남구")
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender("F")
                .nationality("미국")
                .stayDuration("ONE_TO_TWO_M")
                .insuranceStatus("INSURED")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.stayDuration").value("유효한 체류기간 형식이 아닙니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("보험 형태가 null이면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenInsuranceStatusIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .region("서울 강남구")
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender("F")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.insuranceStatus").value("보험 여부는 필수입니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("보험 형태가 올바르지 않으면 400을 반환하고 유저 정보 업데이트에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenInsuranceStatusFormatIsInvalid() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = UpdateUserInfoRequest.builder()
                .name("이름")
                .region("서울 강남구")
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender("F")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("ONE_TO_THREE_M")
                .build();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.insuranceStatus").value("유효한 보험 여부 형식이 아닙니다."));

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertThat(request.getName()).isNotEqualTo(updatedUser.getName());
        assertThat(updatedUser.getName()).isEqualTo(user.getName());

        assertThat(request.getLanguage()).isNotEqualTo(updatedUser.getLanguage().name());
        assertThat(updatedUser.getLanguage()).isEqualTo(user.getLanguage());
    }

    @DisplayName("토큰이 존재하지 않으면 401을 반환하고, 유저 정보 조회에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = getUpdateUserInfoRequest();

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되었으면 401을 반환하고, 유저 정보 조회에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenIsExpired() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);
        UpdateUserInfoRequest request = getUpdateUserInfoRequest();
        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("만료된 access token 입니다."));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private User getUser(UUID userId, UserStatus active) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
                .password("password1234!")
                .status(active)
                .userType(UserType.LOCAL)
                .name("기존 이름")
                .language(Language.ENGLISH)
                .birth(LocalDate.of(2002, 5, 21))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();
    }

    private UpdateUserInfoRequest getUpdateUserInfoRequest() {
        return UpdateUserInfoRequest.builder()
                .name("업데이트 할 이름")
                .language("KOREAN")
                .birthDate(LocalDate.of(2002, 5, 24))
                .gender("F")
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();
    }

}
