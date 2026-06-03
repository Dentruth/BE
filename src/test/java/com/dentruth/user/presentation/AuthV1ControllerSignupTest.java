package com.dentruth.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.common.domain.enums.Language;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.EmailAuthCodeStore;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import com.dentruth.user.presentation.dto.request.SignupRequest;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AuthV1ControllerSignupTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailAuthCodeStore emailAuthCodeStore;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입에 성공한다.")
    @Test
    void shouldSucceedSignUp_successfully() throws Exception {
        //given
        String email = "test@test.com";
        String verifiedToken = UUID.randomUUID().toString();

        given(emailAuthCodeStore.findVerifiedTokenByEmail(email))
                .willReturn(verifiedToken);

        SignupRequest signupRequest = SignupRequest.builder()
                .email(email)
                .password("password1234Test!")
                .name("테스트 유저")
                .language("KOREAN")
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender("F")
                .region("서울시 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .verifiedToken(verifiedToken)
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_201"))
                .andReturn();

        //then
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @DisplayName("이미 가입된 계정이 게스트, 정지, 차단, 정상 계정이면 회원가입에 실패하고, 409를 반환한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideUserStatuses")
    void shouldReturn409Conflict_whenSignUpWithAlreadyRegisteredAccount(String status, UserStatus userStatus)
            throws Exception {
        //given
        String verifiedToken = UUID.randomUUID().toString();
        String email = "test@test.com";

        given(emailAuthCodeStore.findVerifiedTokenByEmail(email))
                .willReturn(verifiedToken);

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("paosdfalsfjlas")
                .name("이미 가입된 유저")
                .language(Language.KOREAN)
                .birth(LocalDate.of(2026, 5, 19))
                .gender(Gender.F)
                .region("서울시 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .status(userStatus)
                .userType(UserType.LOCAL)
                .build());

        SignupRequest signupRequest = SignupRequest.builder()
                .email(email)
                .password("password1234Test!")
                .name("테스트 유저")
                .language("KOREAN")
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender("F")
                .region("서울시 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .verifiedToken(verifiedToken)
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_002"))
                .andExpect(jsonPath("$.message").value("This email is already in use"))
                .andReturn();

        //then
        assertThat(userRepository.count()).isEqualTo(1);
    }

    private static Stream<Arguments> provideUserStatuses() {
        return Stream.of(
                Arguments.of("게스트", UserStatus.GUEST),
                Arguments.of("정지", UserStatus.SUSPENDED),
                Arguments.of("차단", UserStatus.BLOCKED),
                Arguments.of("정상", UserStatus.ACTIVE)
        );
    }

    @DisplayName("이미 가입된 계정이 탈퇴한 계정이거나 삭제된 계정이라면 회원가입에 성공한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideWithdrawableOrDeletedStatuses")
    void shouldSucceedSignUp_whenAccountIsWithdrawnOrDeleted(String status, UserStatus userStatus) throws Exception {
        //given
        String email = "test@test.com";
        String verifiedToken = UUID.randomUUID().toString();

        given(emailAuthCodeStore.findVerifiedTokenByEmail(email))
                .willReturn(verifiedToken);

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("paosdfalsfjlas")
                .name("이미 가입된 유저")
                .language(Language.KOREAN)
                .birth(LocalDate.of(2026, 5, 19))
                .gender(Gender.F)
                .region("서울시 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .status(userStatus)
                .userType(UserType.LOCAL)
                .build());

        SignupRequest signupRequest = SignupRequest.builder()
                .email(email)
                .password("password1234Test!")
                .name("테스트 유저")
                .language("KOREAN")
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender("F")
                .region("서울시 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .verifiedToken(verifiedToken)
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_201"))
                .andReturn();

        //then
        assertThat(userRepository.count()).isEqualTo(2);
    }

    private static Stream<Arguments> provideWithdrawableOrDeletedStatuses() {
        return Stream.of(
                Arguments.of("삭제", UserStatus.DELETED),
                Arguments.of("탈퇴", UserStatus.WITHDRAWN)
        );
    }

    @DisplayName("이메일 인증 토큰이 없으면 회원가입에 실패한다.")
    @Test
    void shouldFailSignUp_whenVerifiedTokenNotFound() throws Exception {
        //given
        String email = "test@test.com";

        given(emailAuthCodeStore.findVerifiedTokenByEmail(email))
                .willReturn("");

        SignupRequest signupRequest = SignupRequest.builder()
                .email(email)
                .password("password1234Test!")
                .name("테스트 유저")
                .language("KOREAN")
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender("F")
                .region("서울시 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .verifiedToken(UUID.randomUUID().toString())
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ErrorStatus.UNAUTHORIZED_EMAIL_VERIFICATION.getMessage()));

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @DisplayName("이메일 인증 토큰이 일치하지 않으면 회원가입에 실패한다.")
    @Test
    void shouldFailSignUp_whenVerifiedTokenMismatch() throws Exception {
        //given
        String email = "test@test.com";

        given(emailAuthCodeStore.findVerifiedTokenByEmail(email))
                .willReturn(UUID.randomUUID().toString());

        SignupRequest signupRequest = SignupRequest.builder()
                .email(email)
                .password("password1234Test!")
                .name("테스트 유저")
                .language("KOREAN")
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender("F")
                .region("서울시 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .verifiedToken(UUID.randomUUID().toString())
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ErrorStatus.UNAUTHORIZED_EMAIL_VERIFICATION.getMessage()));

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @DisplayName("이메일이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenEmailIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().email(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.email").value("Please enter your email"));
    }

    @DisplayName("비밀번호가 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().password(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.password").value("Please enter your password"));
    }

    @DisplayName("이름이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenNameIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().name(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.name").value("Please enter your name"));
    }

    @DisplayName("언어 선택이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenLanguageIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().language(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.language").value("Please select a language"));
    }

    @DisplayName("생년월일이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenBirthDateIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().birthDate(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.birthDate").value("Please select your date of birth"));
    }

    @DisplayName("성별이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenGenderIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().gender(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.gender").value("Please select your gender"));
    }

    @DisplayName("거주지역이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenRegionIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().region(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.region").value("Please select your region"));
    }

    @DisplayName("국적이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenNationalityIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().nationality(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.nationality").value("Please select your nationality"));
    }

    @DisplayName("체류기간이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenStayDurationIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().stayDuration(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.stayDuration").value("Please select your duration of stay"));
    }

    @DisplayName("보험 여부가 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenInsuranceStatusIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().insuranceStatus(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.insuranceStatus").value("Please select your insurance status"));
    }

    @DisplayName("이메일 형식이 정규식에 맞지 않으면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenEmailFormatIsInvalid() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().email("invalid-email-format").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.email").value("Please enter a valid email address"));
    }

    @DisplayName("비밀번호가 요구조건(8~20자)에 맞지 않으면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordFormatIsInvalid1() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().password("!Pw12").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.password").value("Password must be between 8 and 20 characters"));
    }

    @DisplayName("비밀번호가 요구조건(특수문자 + 대문자 + 소문자)에 맞지 않으면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordFormatIsInvalid3() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().password("password1234").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.password").value("Password must include letters, numbers, and special characters"));
    }

    @DisplayName("이름이 2자 미만 또는 50자 초과면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenNameSizeIsInvalid() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().name("김").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.name").value("Name cannot exceed 50 characters"));
    }

    @DisplayName("정해진 언어가 아니라면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenLanguageEnumValuesAreInvalid() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().language("JAPANESE").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.language").value("유효한 언어 선택이 아닙니다."));
    }

    @DisplayName("정해진 성별이 아니라면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenGenderEnumValuesAreInvalid() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().gender("X").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.gender").value("유효한 성별이 아닙니다."));
    }

    @DisplayName("정해진 체류기간이 아니라면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenStayDurationEnumValuesAreInvalid() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().stayDuration("TEN_YEARS").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.stayDuration").value("유효한 체류기간 형식이 아닙니다."));
    }

    @DisplayName("정해진 보험 여부가 아니라면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenInsuranceStatusEnumValuesAreInvalid() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().insuranceStatus("UNKNOWN").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.insuranceStatus").value("유효한 보험 여부 형식이 아닙니다."));
    }

    private SignupRequest createValidSignupRequest() {
        return SignupRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .language("KOREAN")
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender("F")
                .region("서울시 강남구")
                .nationality("미국")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();
    }

}
