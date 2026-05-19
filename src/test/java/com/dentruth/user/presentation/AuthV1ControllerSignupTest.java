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
import com.dentruth.user.presentation.dto.request.SignupRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthV1ControllerSignupTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입에 성공한다.")
    @Test
    void shouldSucceedSignUp_successfully() throws Exception {
        //given
        SignupRequest signupRequest = SignupRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .language("KOREAN")
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender("F")
                .residentialArea("서울시 강남구")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
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
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .password("paosdfalsfjlas")
                .name("이미 가입된 유저")
                .language(Language.KOREAN)
                .birth(LocalDate.of(2026, 5, 19))
                .gender(Gender.F)
                .address("서울시 강남구")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .status(userStatus)
                .userType(UserType.LOCAL)
                .build());

        SignupRequest signupRequest = SignupRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .language("KOREAN")
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender("F")
                .residentialArea("서울시 강남구")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_002"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."))
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
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .password("paosdfalsfjlas")
                .name("이미 가입된 유저")
                .language(Language.KOREAN)
                .birth(LocalDate.of(2026, 5, 19))
                .gender(Gender.F)
                .address("서울시 강남구")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .status(userStatus)
                .userType(UserType.LOCAL)
                .build());

        SignupRequest signupRequest = SignupRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .language("KOREAN")
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender("F")
                .residentialArea("서울시 강남구")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
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
                .andExpect(jsonPath("$.result.email").value("이메일은 필수 입력입니다."));
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
                .andExpect(jsonPath("$.result.password").value("비밀번호는 필수 입력입니다."));
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
                .andExpect(jsonPath("$.result.name").value("이름은 필수 입력입니다."));
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
                .andExpect(jsonPath("$.result.language").value("언어 선택은 필수입니다."));
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
                .andExpect(jsonPath("$.result.birthDate").value("생년월일은 필수입니다."));
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
                .andExpect(jsonPath("$.result.gender").value("성별은 필수입니다."));
    }

    @DisplayName("거주지역이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenResidentialAreaIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().residentialArea(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.residentialArea").value("거주지역은 필수입니다."));
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
                .andExpect(jsonPath("$.result.stayDuration").value("체류기간은 필수입니다."));
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
                .andExpect(jsonPath("$.result.insuranceStatus").value("보험 여부는 필수입니다."));
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
                .andExpect(jsonPath("$.result.email").value("유효한 이메일 형식이 아닙니다."));
    }

    @DisplayName("비밀번호가 요구조건(영문+숫자+특수문자 8~20자)에 맞지 않으면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordFormatIsInvalid() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().password("short12").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result.password").value("유효한 비밀번호 형식이 아닙니다."));
    }

    @DisplayName("이름이 2자 미만 또는 20자 초과면 회원가입에 실패하고 400 에러를 반환한다.")
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
                .andExpect(jsonPath("$.result.name").value("이름은 2~20자 사이여야 합니다."));
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
                .residentialArea("서울시 강남구")
                .stayDuration("ONE_TO_THREE_M")
                .insuranceStatus("INSURED")
                .build();
    }

}
