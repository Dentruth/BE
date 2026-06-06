package com.dentruth.consultprepare.presentaion;

import com.dentruth.common.jwt.CustomUserDetails;
import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.code.SuccessStatus;
import com.dentruth.consultprepare.application.ConsultPrepareService;
import com.dentruth.consultprepare.application.dto.request.CreateConsultCardRequest;
import com.dentruth.consultprepare.application.dto.response.CreateConsultCardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/consult-cards")
public class ConsultCardController {

    private final ConsultPrepareService consultPrepareService;

    @PostMapping
    public ApiResponse<CreateConsultCardResponse> createConsultCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateConsultCardRequest request
    ) {

        return ApiResponse.onSuccess(
                SuccessStatus.CREATED,
                consultPrepareService.createConsultCard(
                        userDetails.getUserId(),
                        request
                )
        );
    }

}
