package com.dentruth.user.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.common.domain.enums.Language;
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

class UserV1ControllerEmailDuplicationTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("이메일 중복 검사시 이메일이 존재하지 않으면 200을 반환한다.")
    @Test
    void shouldReturn200Ok_whenEmailDoesNotExistDuringDuplicateCheck() throws Exception {
        //when, then
        mockMvc.perform(get("/api/v1/users/email/check")
                        .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value("true"))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andExpect(jsonPath("$.result").value("사용 가능"));
    }

    @DisplayName("이메일 중복 검사 시 가입된 이메일이 존재하고 ACTIVE, GUEST, SUSPENDED, BLOCKED 상태라면 409를 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST", "SUSPENDED", "BLOCKED"})
    void shouldReturn409Conflict_whenEmailExistsWithActiveOrRestrictedStatus(UserStatus status) throws Exception {
        //given
        String email = "test@test.com";
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("asdfqwer1234")
                .userType(UserType.LOCAL)
                .birth(LocalDate.of(2002, 01, 01))
                .status(status)
                .region("주소")
                .nationality("국적")
                .name("테스트 유저")
                .gender(Gender.F)
                .insuranceStatus(InsuranceStatus.INSURED)
                .language(Language.KOREAN)
                .build());

        //when, then
        mockMvc.perform(get("/api/v1/users/email/check")
                        .param("email", email))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value("false"))
                .andExpect(jsonPath("$.code").value("USER_002"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."))
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @DisplayName("이메일 중복 검사 시 가입된 이메일이 존재하지만 DELETED, WITHDRAWN 상태라면 200을 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"DELETED", "WITHDRAWN"})
    void shouldReturn200Ok_whenEmailExistsButStatusIsDeletedOrWithdrawn(UserStatus status) throws Exception {
        //given
        String email = "test@test.com";
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("asdfqwer1234")
                .userType(UserType.LOCAL)
                .birth(LocalDate.of(2002, 01, 01))
                .status(status)
                .region("주소")
                .nationality("국적")
                .name("테스트 유저")
                .gender(Gender.F)
                .insuranceStatus(InsuranceStatus.INSURED)
                .language(Language.KOREAN)
                .build());

        //when, then
        mockMvc.perform(get("/api/v1/users/email/check")
                        .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value("true"))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andExpect(jsonPath("$.result").value("사용 가능"));
    }

}
