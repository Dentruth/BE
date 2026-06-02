package com.dentruth.consultsummary.presentation;

import com.dentruth.common.jwt.CustomUserDetails;
import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.CursorResponse;
import com.dentruth.common.response.code.SuccessStatus;
import com.dentruth.consultsummary.application.ConsultSummaryFacade;
import com.dentruth.consultsummary.application.dto.response.ConsultSummariesResponse;
import com.dentruth.consultsummary.application.dto.response.CreateConsultSummaryResponse;
import com.dentruth.consultsummary.application.dto.response.GetConsultSummaryResponse;
import com.dentruth.consultsummary.application.dto.response.PresignedUrlResponse;
import com.dentruth.consultsummary.presentation.dto.request.CreateConsultSummaryRequest;
import com.dentruth.consultsummary.presentation.dto.request.UpdateSummaryRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/consult-summaries")
public class ConsultSummaryV1Controller {

    private final ConsultSummaryFacade consultSummaryFacade;

    @GetMapping("/upload-url")
    @Operation(summary = "음성 파일 업로드용 Presigned URL 발급")
    public ApiResponse<PresignedUrlResponse> getUploadUrl(@RequestParam String filename,
                                                          @RequestParam String contentType,
                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        PresignedUrlResponse response = consultSummaryFacade.getUploadUrl(filename, contentType, userId);
        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }

    @PostMapping
    @Operation(summary = "상담 기록 생성")
    public ResponseEntity<ApiResponse<CreateConsultSummaryResponse>> createConsultSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateConsultSummaryRequest request) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ResponseEntity.accepted().body(ApiResponse.onSuccess(
                SuccessStatus.ACCEPTED,
                consultSummaryFacade.createConsultSummary(userId, request.toApplicationRequest())
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ai 요약 상세 조회")
    public ApiResponse<GetConsultSummaryResponse> getConsultSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ApiResponse.onSuccess(SuccessStatus.OK, consultSummaryFacade.getDetail(userId, id));
    }

    @GetMapping
    @Operation(summary = "ai 요약 전체 조회")
    public ApiResponse<CursorResponse<ConsultSummariesResponse>> getAllConsultSummaries(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ApiResponse.onSuccess(SuccessStatus.OK, consultSummaryFacade.getConsultSummaries(userId, cursor, size));
    }

    @DeleteMapping
    @Operation(summary = "ai 요약 선택 삭제")
    public ResponseEntity<Void> deleteSummaries(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @RequestParam("summaryIds") List<UUID> summaryIds) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        consultSummaryFacade.deleteSummaries(userId, summaryIds);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "ai 요약 수정")
    public ApiResponse<GetConsultSummaryResponse> updateSummary(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                @PathVariable UUID id,
                                                                @Valid @RequestBody UpdateSummaryRequest request) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ApiResponse.onSuccess(SuccessStatus.OK,
                consultSummaryFacade.updateSummary(userId, id, request.toApplicationRequest()));
    }

}
