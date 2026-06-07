package com.dentruth.consultprepare.presentaion;

import com.dentruth.common.jwt.CustomUserDetails;
import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.code.SuccessStatus;
import com.dentruth.consultprepare.application.ConsultPrepareService;
import com.dentruth.consultprepare.application.dto.request.CreateConsultCardRequest;
import com.dentruth.consultprepare.application.dto.response.ConsultCardListItemResponse;
import com.dentruth.consultprepare.application.dto.response.CreateConsultCardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.UUID;

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
                        UUID.fromString(userDetails.getUserId()),
                        request
                )
        );
    }

    @GetMapping
    public ApiResponse<List<ConsultCardListItemResponse.ConsultCardListItemResponse>> getConsultCards(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ApiResponse.onSuccess(
                SuccessStatus.OK,
                consultPrepareService.getConsultCards(
                        userDetails.getUserId()
                )
        );
    }

}
