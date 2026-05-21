package com.dentruth.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import com.dentruth.user.infra.redis.RedisTokenStore;
import com.dentruth.user.presentation.dto.request.LoginRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

class AuthV1ControllerLoginTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTokenStore redisTokenStore;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("유저가 존재하고 ACTIVE 상태이며, 비밀번호가 일치하면 로그인에 성공한다.")
    @Test
    void shouldSucceedLogin_whenUserExistsAndIsActiveAndPasswordMatches() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = passwordEncoder.encode(password);

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value("true"))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.result.refreshToken").isNotEmpty())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        String responseRefreshToken = com.jayway.jsonpath.JsonPath.read(json, "$.result.refreshToken");

        String storedRefreshToken = redisTokenStore.findByUserId(userId);
        assertThat(storedRefreshToken).isNotNull();
        assertThat(storedRefreshToken).isEqualTo(responseRefreshToken);
    }

    @DisplayName("유저가 존재하지만 SUSPENDED 상태면 로그인에 실패하고 403을 반환한다.")
    @Test
    void shouldReturn403Forbidden_whenUserExistsButStatusIsSuspended() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = passwordEncoder.encode(password);

        saveUser(userId, email, encodePassword, UserStatus.SUSPENDED);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("일시 정지된 계정입니다."));
    }

    @DisplayName("유저가 존재하지만 BLOCKED 상태면 로그인에 실패하고 403을 반환한다.")
    @Test
    void shouldReturn403Forbidden_whenUserExistsButStatusIsBlocked() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = passwordEncoder.encode(password);

        saveUser(userId, email, encodePassword, UserStatus.BLOCKED);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("차단된 계정입니다."));
    }

    @DisplayName("유저가 존재하지만 WITHDRAWN 상태면 로그인에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404NotFound_whenUserExistsButStatusIsWithdrawn() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = passwordEncoder.encode(password);

        saveUser(userId, email, encodePassword, UserStatus.WITHDRAWN);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));
    }

    @DisplayName("유저가 존재하지만 DELETED 상태면 로그인에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404NotFound_whenUserExistsButStatusIsDeleted() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = passwordEncoder.encode(password);

        saveUser(userId, email, encodePassword, UserStatus.DELETED);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));
    }

    @DisplayName("유저가 존재하지 않으면 로그인에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404NotFound_whenUserDoesNotExist() throws Exception {
        //given
        String email = "test@test.com";
        String password = "password1234!!";

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));
    }

    @DisplayName("유저가 존재하고 ACTIVE 상태지만, 비밀번호가 일치하지 않으면 로그인에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenUserExistsAndIsActiveButPasswordDoesNotMatch() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("USER_004"))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
    }

    @DisplayName("이메일을 입력하지 않고 로그인을 시도하면 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenLoginWithoutEmail() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.email").value("이메일은 필수 입력입니다."));

    }

    @DisplayName("비밀번호를 입력하지 않고 로그인을 시도하면 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenLoginWithoutPassword() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.password").value("비밀번호는 필수 입력입니다."));

    }

    @DisplayName("이메일 형식이 정규식에 맞지 않으면 로그인에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenEmailFormatIsInvalid() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "invalid-email-format";
        String password = "password1234!!";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.email").value("유효한 이메일 형식이 아닙니다."));
    }

    @DisplayName("비밀번호가 요구조건(영문+숫자+특수문자 8~20자)에 맞지 않으면 로그인에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordDoesNotMatchRequirements() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "short12";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.password").value("유효한 비밀번호 형식이 아닙니다."));
    }

    private void saveUser(UUID userId, String email, String encodePassword, UserStatus deleted) {
        userRepository.save(User.builder()
                .id(userId)
                .email(email)
                .password(encodePassword)
                .name("이미 가입된 유저")
                .language(Language.KOREAN)
                .birth(LocalDate.of(2026, 5, 19))
                .gender(Gender.F)
                .address("서울시 강남구")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .status(deleted)
                .userType(UserType.LOCAL)
                .build());
    }

}
