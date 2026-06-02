package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.dentruth.common.response.CursorResponse;
import com.dentruth.consultsummary.application.dto.response.ConsultSummariesResponse;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.dentruth.user.application.UserService;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.UserStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsultSummaryFacadeGetConsultSummariesTest {

    @InjectMocks
    private ConsultSummaryFacade consultSummaryFacade;

    @Mock
    private ConsultSummaryService consultSummaryService;

    @Mock
    private UserService userService;

    @Nested
    @DisplayName("상담 요약 내역 커서 페이징 조회 서비스 흐름 테스트")
    class GetConsultSummariesTest {

        private final UUID userId = UUID.randomUUID();
        private final int size = 2;

        @DisplayName("조회된 데이터의 개수가 요청한 size보다 크다면 hasNext는 true이고, 마지막 아이템의 ID가 차기 커서가 된다.")
        @Test
        void getConsultSummaries_hasNextPage_success() {
            //given
            UUID cursorId = null;

            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();

            User mockUser = User.builder().id(userId).status(UserStatus.ACTIVE).build();

            List<ConsultSummary> mockSummaries = new ArrayList<>(List.of(
                    createMockSummary(id1, "강남 치과", "충치치료"),
                    createMockSummary(id2, "역삼 치과", "임플란트"),
                    createMockSummary(id3, "선릉 치과", "교정치료")
            ));

            given(userService.findById(userId, "ai 요약 기록 전체 조회")).willReturn(mockUser);
            given(consultSummaryService.findAllSummaries(userId, cursorId, size)).willReturn(mockSummaries);

            //when
            CursorResponse<ConsultSummariesResponse> response = consultSummaryFacade.getConsultSummaries(userId,
                    cursorId, size);

            //then
            assertThat(response.isHasNext()).isTrue();
            assertThat(response.getItems()).hasSize(2);

            assertThat(response.getNextCursor()).isEqualTo(id2.toString());

            assertThat(response.getItems().get(0).getClinicName()).isEqualTo("강남 치과");
            assertThat(response.getItems().get(1).getClinicName()).isEqualTo("역삼 치과");

            verify(userService).findById(userId, "ai 요약 기록 전체 조회");
            verify(consultSummaryService).findAllSummaries(userId, cursorId, size);
        }

        @DisplayName("조회된 데이터의 개수가 요청한 size와 같거나 작다면 hasNext는 false이고, 차기 커서는 null이 된다.")
        @Test
        void getConsultSummaries_noNextPage_success() {
            //given
            UUID cursorId = UUID.randomUUID();

            User mockUser = User.builder().id(userId).status(UserStatus.ACTIVE).build();

            UUID id1 = UUID.randomUUID();
            List<ConsultSummary> mockSummaries = new ArrayList<>(List.of(
                    createMockSummary(id1, "종로 치과", "신경치료")
            ));

            given(userService.findById(userId, "ai 요약 기록 전체 조회")).willReturn(mockUser);
            given(consultSummaryService.findAllSummaries(userId, cursorId, size)).willReturn(mockSummaries);

            //when
            CursorResponse<ConsultSummariesResponse> response =
                    consultSummaryFacade.getConsultSummaries(userId, cursorId, size);

            //then
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getNextCursor()).isNull();

            assertThat(response.getItems().get(0).getClinicName()).isEqualTo("종로 치과");

            verify(userService).findById(userId, "ai 요약 기록 전체 조회");
            verify(consultSummaryService).findAllSummaries(userId, cursorId, size);
        }
    }

    private ConsultSummary createMockSummary(UUID id, String clinicName, String title) {
        ConsultSummary summary = ConsultSummary.builder().build();
        ReflectionTestUtils.setField(summary, "id", id);
        ReflectionTestUtils.setField(summary, "clinicName", clinicName);
        ReflectionTestUtils.setField(summary, "title", title);
        ReflectionTestUtils.setField(summary, "status", SummaryStatus.COMPLETED);
        ReflectionTestUtils.setField(summary, "createdAt", Instant.now());
        return summary;
    }

}

