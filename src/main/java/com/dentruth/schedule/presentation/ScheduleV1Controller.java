package com.dentruth.schedule.presentation;

import com.dentruth.common.jwt.CustomUserDetails;
import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.code.SuccessStatus;
import com.dentruth.schedule.application.ScheduleService;
import com.dentruth.schedule.application.dto.request.CreateScheduleRequest;
import com.dentruth.schedule.application.dto.request.UpdateScheduleRequest;
import com.dentruth.schedule.application.dto.response.CreateScheduleResponse;
import com.dentruth.schedule.application.dto.response.ScheduleDetailResponse;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleV1Controller {

    private final ScheduleService scheduleService;

    @PostMapping
    public ApiResponse<CreateScheduleResponse> createSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateScheduleRequest request
    ) {

        return ApiResponse.onSuccess(
                SuccessStatus.OK,
                scheduleService.createSchedule(
                        UUID.fromString(userDetails.getUserId()),
                        request
                )
        );
    }

    @PutMapping("/{scheduleId}")
    public ApiResponse<ScheduleDetailResponse> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request
    ) {

        return ApiResponse.onSuccess(
                SuccessStatus.OK,
                scheduleService.updateSchedule(scheduleId, request)
        );
    }

    @DeleteMapping("/{scheduleId}")
    public ApiResponse<Void> deleteSchedule(
            @PathVariable Long scheduleId
    ) {

        scheduleService.deleteSchedule(scheduleId);

        return ApiResponse.onSuccess(SuccessStatus.OK, null);
    }

    @GetMapping("/{scheduleId}")
    public ApiResponse<ScheduleDetailResponse> getSchedule(
            @PathVariable Long scheduleId
    ) {

        return ApiResponse.onSuccess(
                SuccessStatus.OK,
                scheduleService.getSchedule(scheduleId)
        );
    }
}
