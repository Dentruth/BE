package com.dentruth.consultsummary.application;

import com.dentruth.consultsummary.application.dto.response.PresignedUrlResponse;
import com.dentruth.user.application.UserService;
import com.dentruth.user.domain.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultSummaryFacade {

    private final PresignedUrlService presignedUrlService;
    private final UserService userService;

    public PresignedUrlResponse getUploadUrl(String filename, String contentType, UUID userId){
        log.info("Presigned URL 발급 요청. filename: [{}], contentType: [{}], User Id : [{}]",
                filename, contentType, userId);

        User user = userService.findById(userId, "Presigned URL 발급 요청");
        user.validateStatus();

        return presignedUrlService.generateUploadUrl(filename, contentType, userId);
    }

}
