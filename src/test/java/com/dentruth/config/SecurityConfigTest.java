package com.dentruth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("회원가입은 인증 없이 접근 가능하다.")
    @Test
    void shouldAllowAccessToSignUp_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("로그인은 인증 없이 접근 가능하다.")
    @Test
    void shouldAllowAccessToLogin_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("소셜 로그인은 인증 없이 접근 가능하다.")
    @Test
    void shouldAllowAccessToGoogleLogin_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(get("/api/v1/auth/login/google"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("토큰 재발급은 인증 없이 접근 가능하다.")
    @Test
    void shouldAllowAccessToRefreshToken_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("이메일 인증 요청은 인증 없이 접근 가능하다.")
    @Test
    void shouldAllowAccessToEmailVerification_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(post("/api/v1/users/email/verification"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("이메일 중복 검사는 인증 없이 접근 가능하다.")
    @Test
    void shouldAllowAccessToEmailCheck_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(get("/api/v1/users/email/check"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("Swagger는 인증 없이 접근 가능하다.")
    @Test
    void shouldAllowAccessToSwagger_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("회원 탈퇴는 인증 없이 접근이 불가능하다.")
    @Test
    void shouldDenyAccessToWithdrawal_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("회원 정보 조회는 인증 없이 접근이 불가능하다.")
    @Test
    void shouldDenyAccessToGetUserInfo_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("비밀번호 수정은 인증 없이 접근이 불가능하다.")
    @Test
    void shouldDenyAccessToUpdatePassword_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(patch("/api/v1/users/me/password"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("로그아웃은 인증 없이 접근이 불가능하다.")
    @Test
    void shouldDenyAccessToLogout_withoutAuthentication() throws Exception {
        //when, then
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("허용된 Origin(localhost:3000)은 CORS를 통과한다.")
    @Test
    void shouldAllowCors_fromAllowedOrigin() throws Exception {
        //when, then
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @DisplayName("허용되지 않은 Origin은 CORS를 통과하지 못한다.")
    @Test
    void shouldDenyCors_fromNotAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://testttt.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

}
