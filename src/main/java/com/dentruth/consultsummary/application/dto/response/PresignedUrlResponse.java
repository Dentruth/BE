package com.dentruth.consultsummary.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponse {

    private final String presignedUrl;
    private final String s3Key;
    private final int expiresIn;

}
