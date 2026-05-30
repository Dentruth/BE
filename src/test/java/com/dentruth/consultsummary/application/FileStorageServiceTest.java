package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.dto.response.PresignedUrlResponse;
import com.dentruth.consultsummary.infra.s3.S3FileStorageService;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @InjectMocks
    private S3FileStorageService s3PresignedUrlService;

    @Mock
    private S3Presigner s3Presigner;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Field bucketField = S3FileStorageService.class.getDeclaredField("bucket");
        bucketField.setAccessible(true);
        bucketField.set(s3PresignedUrlService, "test-bucket");
    }

    @DisplayName("유효한 파일명과 ContentType으로 Presigned URL을 발급받을 수 있다.")
    @Test
    void shouldGeneratePresignedUrl_whenValidRequest() throws MalformedURLException {
        //given
        String filename = "consultation.m4a";
        String contentType = "audio/mp4";

        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        given(mockPresignedRequest.url()).willReturn(
                URI.create("https://s3.com/test-bucket/consultations/test.m4a").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(mockPresignedRequest);

        //when
        PresignedUrlResponse response = s3PresignedUrlService.generateUploadUrl(filename, contentType, USER_ID);

        //then
        assertThat(response.getPresignedUrl()).contains("s3.com");
        assertThat(response.getS3Key()).startsWith("consultations/" + USER_ID + "/");
        assertThat(response.getS3Key()).endsWith(".m4a");
        assertThat(response.getExpiresIn()).isEqualTo(300);
    }

    @DisplayName("지원하지 않는 contentType이면 예외를 던진다.")
    @Test
    void shouldThrowException_whenInvalidContentType() {
        //given
        String filename = "consultation.m4a";
        String contentType = "text/plain";

        //when, then
        assertThatThrownBy(() -> s3PresignedUrlService.generateUploadUrl(filename, contentType, USER_ID))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.INVALID_FILE_TYPE.getMessage());

        verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @DisplayName("확장자가 없는 파일명이면 예외를 던진다.")
    @Test
    void shouldThrowException_whenFilenameHasNoExtension() {
        //given
        String filename = "consultation";
        String contentType = "text/plain";

        //when, then
        assertThatThrownBy(() -> s3PresignedUrlService.generateUploadUrl(filename, contentType, USER_ID))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.INVALID_FILE_TYPE.getMessage());

        verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @ParameterizedTest(name = "[{index}] contentType: {0}")
    @ValueSource(strings = {"audio/mpeg", "audio/mp4", "audio/webm", "audio/wav", "audio/ogg"})
    @DisplayName("허용된 모든 contentType으로 Presigned URL을 발급할 수 있다.")
    void shouldGeneratePresignedUrl_whenAllAllowedContentTypes(String contentType) throws Exception {
        //given
        String filename = "consultation.m4a";

        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        given(mockPresignedRequest.url()).willReturn(
                URI.create("https://s3.com/test-bucket/consultations/test.m4a").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(mockPresignedRequest);

        //when, then
        assertThatNoException().isThrownBy(
                () -> s3PresignedUrlService.generateUploadUrl(filename, contentType, USER_ID)
        );
    }

}
