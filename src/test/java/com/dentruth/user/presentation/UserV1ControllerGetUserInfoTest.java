package com.dentruth.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MvcResult;

class UserV1ControllerGetUserInfoTest extends ControllerTestSupport {

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

    @DisplayName("유저 정보가 존재하고, GUEST, ACTIVE 상태라면 조회에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldSucceedRetrieval_whenUserExistsAndStatusIsActiveOrGuest(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value("true"))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andReturn();

        User user = userRepository.findById(userId)
                .orElseThrow();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode resultNode = objectMapper.readTree(jsonResponse).get("result");

        assertThat(resultNode.get("name").asText()).isEqualTo(user.getName());
        assertThat(resultNode.get("language").asText()).isEqualTo(user.getLanguage().name());
        assertThat(resultNode.get("birth").asText()).isEqualTo(user.getBirth().toString());
        assertThat(resultNode.get("gender").asText()).isEqualTo(user.getGender().getKo());
        assertThat(resultNode.get("region").asText()).isEqualTo(user.getRegion());
        assertThat(resultNode.get("stayDuration").asText()).isEqualTo(user.getStayDuration().getKo());
        assertThat(resultNode.get("insuranceStatus").asText()).isEqualTo(user.getInsuranceStatus().getKo());
        assertThat(resultNode.get("nationality").asText()).isEqualTo(user.getNationality());
    }

    @DisplayName("Language가 ENGLISH거나 SPANISH면 영어로 결과가 반환된다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = Language.class, names = {"ENGLISH", "SPANISH"})
    void shouldReturnInfoInEnglish_whenLanguageIsEnglishOrSpanish(Language language) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, language, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value("true"))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andReturn();

        User user = userRepository.findById(userId)
                .orElseThrow();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode resultNode = objectMapper.readTree(jsonResponse).get("result");

        assertThat(resultNode.get("name").asText()).isEqualTo(user.getName());
        assertThat(resultNode.get("language").asText()).isEqualTo(user.getLanguage().name());
        assertThat(resultNode.get("birth").asText()).isEqualTo(user.getBirth().toString());
        assertThat(resultNode.get("gender").asText()).isEqualTo(user.getGender().getEng());
        assertThat(resultNode.get("region").asText()).isEqualTo(user.getRegion());
        assertThat(resultNode.get("stayDuration").asText()).isEqualTo(user.getStayDuration().getEng());
        assertThat(resultNode.get("insuranceStatus").asText()).isEqualTo(user.getInsuranceStatus().getEng());
        assertThat(resultNode.get("nationality").asText()).isEqualTo(user.getNationality());
    }

    @DisplayName("Language가 KOREAN이면 한국어로 결과가 반환된다.")
    @Test
    void shouldReturnInfoInKorean_whenLanguageIsKorean() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value("true"))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andReturn();

        User user = userRepository.findById(userId)
                .orElseThrow();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode resultNode = objectMapper.readTree(jsonResponse).get("result");

        assertThat(resultNode.get("name").asText()).isEqualTo(user.getName());
        assertThat(resultNode.get("language").asText()).isEqualTo(user.getLanguage().name());
        assertThat(resultNode.get("birth").asText()).isEqualTo(user.getBirth().toString());
        assertThat(resultNode.get("gender").asText()).isEqualTo(user.getGender().getKo());
        assertThat(resultNode.get("region").asText()).isEqualTo(user.getRegion());
        assertThat(resultNode.get("stayDuration").asText()).isEqualTo(user.getStayDuration().getKo());
        assertThat(resultNode.get("insuranceStatus").asText()).isEqualTo(user.getInsuranceStatus().getKo());
        assertThat(resultNode.get("nationality").asText()).isEqualTo(user.getNationality());
    }

    @DisplayName("유저 정보가 존재하지만 WITHDRAWN이거나 DELETED이면 404를 반환하고, 유저 정보 조회에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404NotFound_whenUserExistsButStatusIsWithdrawnOrDeleted(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."))
                .andReturn();
    }

    @DisplayName("유저 정보가 존재하지만 SUSPENDED이면 403을 반환하고, 유저 정보 조회에 실패한다.")
    @Test
    void shouldReturn403_whenUserExistsButStatusIsSuspended() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.SUSPENDED));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("일시 정지된 계정입니다."))
                .andReturn();
    }

    @DisplayName("유저 정보가 존재하지만 BLOCKED이면 403을 반환하고, 유저 정보 조회에 실패한다.")
    @Test
    void shouldReturn403_whenUserExistsButStatusIsBlocked() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.BLOCKED));
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("차단된 계정입니다."))
                .andReturn();
    }

    @DisplayName("유저 정보가 존재하지 않으면 404를 반환하고, 유저 정보 조회에 실패한다.")
    @Test
    void shouldReturn404NotFound_whenUserDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString(), Language.KOREAN.name());

        //when
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("회원 정보가 없습니다."))
                .andReturn();
    }

    @DisplayName("토큰이 존재하지 않으면 401을 반환하고, 유저 정보 조회에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, Language.KOREAN, UserStatus.BLOCKED));

        //when
        mockMvc.perform(get("/api/v1/users/me"))
                //then
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되었으면 401을 반환하고, 유저 정보 조회에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenIsExpired() throws Exception {
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

        //when
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("만료된 access token 입니다."));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private User getUser(UUID userId, Language language, UserStatus userStatus) {
        return User.builder()
                .id(userId)
                .region("Region")
                .nationality("nationality")
                .email("test@test.com")
                .password("password12345!")
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
