package com.dentruth.consultsummary.presentation;

import com.dentruth.common.jwt.CustomUserDetails;
import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.code.SuccessStatus;
import com.dentruth.consultsummary.application.ConsultSummaryFacade;
import com.dentruth.consultsummary.application.dto.response.CreateConsultSummaryResponse;
import com.dentruth.consultsummary.presentation.dto.request.CreateConsultSummaryV2Request;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/consult-summaries")
public class ConsultSummaryV2Controller {

    private final ConsultSummaryFacade consultSummaryFacade;

    @PostMapping
    @Operation(summary = "상담 기록 생성")
    public ResponseEntity<ApiResponse<CreateConsultSummaryResponse>> createConsultSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateConsultSummaryV2Request request) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ResponseEntity.accepted().body(ApiResponse.onSuccess(
                SuccessStatus.ACCEPTED,
                consultSummaryFacade.createConsultSummaryV2(userId, request.toApplicationRequest())
        ));
    }

}
