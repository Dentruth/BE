package com.dentruth.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.common.jwt.JwtProperties;
import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.common.domain.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import com.dentruth.user.presentation.dto.request.UpdatePasswordRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

class UserV1ControllerUpdatePasswordTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    private static final String existingPassword = "password1234!";

    @DisplayName("유저 정보가 존재하고 GUEST, ACTIVE 상태이며 기존 비밀번호가 일치하면 비밀번호 변경에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldChangePasswordSuccessfully_whenUserExistsAndIsActiveOrGuestAndPasswordMatches(UserStatus userStatus)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value("true"))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."));

        User user = userRepository.findById(userId)
                .orElseThrow();

        assertThat(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).isTrue();
    }

    @DisplayName("유저 정보가 존재하고 GUEST, ACTIVE 상태지만 기존 비밀번호가 일치하지 않으면 400을 반환하고 비밀번호 변경에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldReturn400BadRequest_whenCurrentPasswordDoesNotMatchDuringPasswordChange(UserStatus userStatus)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword + "1234asdf")
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_004"))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));

        User user = userRepository.findById(userId)
                .orElseThrow();

        assertThat(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).isFalse();
    }

    @DisplayName("유저 정보가 존재하지만 WITHDRAWN이거나 DELETED이면 404를 반환하고, 비밀번호 변경에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404NotFound_whenUserExistsButStatusIsWithdrawnOrDeleted(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."))
                .andReturn();
    }

    @DisplayName("유저 정보가 존재하지만 SUSPENDED이면 403을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn403Forbidden_whenUserExistsButStatusIsSuspendedDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.SUSPENDED));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("일시 정지된 계정입니다."))
                .andReturn();
    }

    @DisplayName("유저 정보가 존재하지만 BLOCKED이면 403을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn403Forbidden_whenUserExistsButStatusIsBlockedDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.BLOCKED));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("차단된 계정입니다."))
                .andReturn();
    }

    @DisplayName("유저 정보가 존재하지 않으면 404를 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn404NotFound_whenUserDoesNotExistDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."))
                .andReturn();
    }

    @DisplayName("토큰이 존재하지 않으면 401을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenDoesNotExistDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.BLOCKED));

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되었으면 401을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenIsExpiredDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.ACTIVE));
        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("만료된 access token 입니다."));
    }

    @DisplayName("기존 비밀번호가 null이면 400을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenCurrentPasswordIsNullDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.existingPassword").value("기존 비밀번호는 필수 입력입니다."))
                .andReturn();
    }

    @DisplayName("변경할 비밀번호가 null이면 400을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenNewPasswordIsNullDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.newPassword").value("변경할 비밀번호는 필수 입력입니다."))
                .andReturn();
    }

    @DisplayName("변경할 비밀번호의 형식이 유효하지 않으면 400을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenNewPasswordFormatIsInvalidDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdf1234")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result.newPassword").value("유효한 비밀번호 형식이 아닙니다."))
                .andReturn();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private User getUser(UUID userId, Language language, UserStatus userStatus) {
        String encodedPassword = passwordEncoder.encode(existingPassword);
        return User.builder()
                .id(userId)
                .region("Region")
                .nationality("nationality")
                .email("test@test.com")
                .password(encodedPassword)
                .userType(UserType.LOCAL)
                .name("name")
                .birth(LocalDate.of(2002, 5, 24))
                .gender(Gender.F)
                .language(language)
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .status(userStatus)
                .build();
    }

}
