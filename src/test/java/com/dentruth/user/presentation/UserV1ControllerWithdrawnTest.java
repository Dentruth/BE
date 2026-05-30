package com.dentruth.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.dentruth.user.presentation.dto.request.WithdrawnRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserV1ControllerWithdrawnTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @DisplayName("유저가 ACTIVE 상태거나 GUEST 상태이고 비밀번호가 일치하면 탈퇴에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldWithdrawSuccessfully_whenUserIsActiveOrGuestAndPasswordMatches(UserStatus status) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");

        User user = getUser(userId, encodedPassword, status);

        userRepository.save(user);

        WithdrawnRequest request = new WithdrawnRequest("password1234!");

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        //then
        User after = userRepository.findById(userId)
                .orElseThrow();

        assertThat(after.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(after.getDeletedAt()).isNotNull();
    }

    @DisplayName("유저가 ACTIVE 상태거나 GUEST 상태지만 비밀번호가 일치하지 않으면 400을 반환하고, 탈퇴에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldReturn400BadRequest_whenPasswordDoesNotMatchDuringWithdrawal(UserStatus status) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");

        User user = getUser(userId, encodedPassword, status);

        userRepository.save(user);

        WithdrawnRequest request = new WithdrawnRequest("asdfqwer1234!");

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_004"))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));

        User after = userRepository.findById(userId)
                .orElseThrow();

        assertThat(after.getStatus()).isEqualTo(status);
        assertThat(after.getDeletedAt()).isNull();
    }

    @DisplayName("유저가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404NotFound_whenUserDoesNotExistDuringWithdrawal() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        WithdrawnRequest request = new WithdrawnRequest("asdfqwer1234!");

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));
    }

    @DisplayName("유저가 WITHDRAWN 상태거나 DELETED 상태면 404를 반환하고, 탈퇴에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404NotFound_whenUserIsAlreadyWithdrawnOrDeletedDuringWithdrawal(UserStatus status)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");

        User user = getUser(userId, encodedPassword, status);

        userRepository.save(user);

        WithdrawnRequest request = new WithdrawnRequest("password1234!");

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."));

        User after = userRepository.findById(userId)
                .orElseThrow();

        assertThat(after.getStatus()).isEqualTo(status);
        assertThat(after.getDeletedAt()).isNull();
    }

    @DisplayName("유저가 BLOCKED 상태면 403을 반환하고, 탈퇴에 실패한다.")
    @Test
    void shouldReturn403Unauthorized_whenUserIsBlockedDuringWithdrawal() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");

        User user = getUser(userId, encodedPassword, UserStatus.BLOCKED);

        userRepository.save(user);

        WithdrawnRequest request = new WithdrawnRequest("password1234!");

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("차단된 계정입니다."));

        User after = userRepository.findById(userId)
                .orElseThrow();

        assertThat(after.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(after.getDeletedAt()).isNull();
    }

    @DisplayName("유저가 SUSPENDED 상태면 401을 반환하고, 탈퇴에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenUserIsSuspendedDuringWithdrawal() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");

        User user = getUser(userId, encodedPassword, UserStatus.SUSPENDED);
        userRepository.save(user);

        WithdrawnRequest request = new WithdrawnRequest("password1234!");

        String token = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("일시 정지된 계정입니다."));

        User after = userRepository.findById(userId)
                .orElseThrow();

        assertThat(after.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(after.getDeletedAt()).isNull();
    }

    @DisplayName("토큰이 존재하지 않으면 401을 반환하고, 유저 정보 조회에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");
        User user = getUser(userId, encodedPassword, UserStatus.ACTIVE);
        userRepository.save(user);

        //when
        mockMvc.perform(delete("/api/v1/users/me"))
                //then
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되었으면 401을 반환하고, 유저 정보 조회에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenIsExpired() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");
        User user = getUser(userId, encodedPassword, UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("만료된 access token 입니다."));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private User getUser(UUID userId, String encodedPassword, UserStatus suspended) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
                .password(encodedPassword)
                .status(suspended)
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
