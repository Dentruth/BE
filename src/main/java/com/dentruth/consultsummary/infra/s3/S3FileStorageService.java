package com.dentruth.consultsummary.infra.s3;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.FileStorageService;
import com.dentruth.consultsummary.application.dto.response.PresignedUrlResponse;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageService implements FileStorageService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private static final int EXPIRES_IN_SECONDS = 300;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp4",
            "audio/webm",
            "audio/wav",
            "audio/ogg"
    );

    @Override
    public PresignedUrlResponse generateUploadUrl(String filename, String contentType, UUID userId) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new DentruthException(ErrorStatus.INVALID_FILE_TYPE);
        }
        String s3Key = generateS3Key(filename, userId);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(EXPIRES_IN_SECONDS))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        log.info("Presigned URL 발급 완료. s3Key : [{}]", s3Key);

        return PresignedUrlResponse.builder()
                .presignedUrl(presignedRequest.url().toString())
                .s3Key(s3Key)
                .expiresIn(EXPIRES_IN_SECONDS)
                .build();
    }

    @Override
    public InputStream streamAudio(String s3Key) {
        ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .build()
        );

        log.info("S3 오디오 스트림 오픈. S3Key : [{}]", s3Key);
        return s3Stream;
    }

    private String generateS3Key(String filename, UUID userId) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            throw new DentruthException(ErrorStatus.INVALID_FILE_TYPE);
        }
        String extension = filename.substring(dotIndex);
        return "consultations/" + userId + "/" + UUID.randomUUID() + extension;
    }

}
