package com.dentruth.consultsummary.presentation;

import com.dentruth.common.jwt.CustomUserDetails;
import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.code.SuccessStatus;
import com.dentruth.consultsummary.application.ConsultSummaryFacade;
import com.dentruth.consultsummary.application.dto.response.CreateConsultSummaryResponse;
import com.dentruth.consultsummary.application.dto.response.PresignedUrlResponse;
import com.dentruth.consultsummary.presentation.dto.reqeust.CreateConsultSummaryRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

}
