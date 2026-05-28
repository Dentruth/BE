package com.dentruth.consultsummary.application;

import com.dentruth.consultsummary.application.dto.response.PresignedUrlResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface FileStorageService {

    PresignedUrlResponse generateUploadUrl(String filename, String contentType, UUID userId);
    InputStream streamAudio(String s3Key) throws IOException;

}
