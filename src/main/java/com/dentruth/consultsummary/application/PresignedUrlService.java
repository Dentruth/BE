package com.dentruth.consultsummary.application;

import com.dentruth.consultsummary.application.dto.response.PresignedUrlResponse;
import java.util.UUID;

public interface PresignedUrlService {

    PresignedUrlResponse generateUploadUrl(String filename, String contentType, UUID userId);

}
