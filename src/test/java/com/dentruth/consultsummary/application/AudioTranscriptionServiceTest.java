package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.infra.ai.WhisperAudioTranscriptionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AudioTranscriptionServiceTest {

    @InjectMocks
    private WhisperAudioTranscriptionService audioTranscriptionService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private RestTemplate restTemplate;

    private final String s3Key = "audio/consult_20260527.mp3";
    private final String expectedText = "안녕하세요. 치과 진료 시작하겠습니다.";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(audioTranscriptionService, "apiKey", "mock-api-key");
        ReflectionTestUtils.setField(audioTranscriptionService, "audioModel", "whisper-1");
        ReflectionTestUtils.setField(audioTranscriptionService, "whisperUrl",
                "https://api.openai.com/v1/audio/transcriptions");
    }

    @DisplayName("유효한 S3 키가 주어지면 S3에서 오디오 스트림을 가져와 Whisper STT 변환에 성공한다.")
    @Test
    void shouldConvertSttSuccessfully_whenS3KeyExists() throws IOException {
        //given
        InputStream mockAudioStream = new ByteArrayInputStream("mock audio data".getBytes());
        given(fileStorageService.streamAudio(s3Key)).willReturn(mockAudioStream);

        Map<String, String> responseBody = Map.of("text", expectedText);
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(responseBody);

        given(restTemplate.postForEntity(
                eq("https://api.openai.com/v1/audio/transcriptions"),
                any(HttpEntity.class),
                eq(Map.class))
        ).willReturn(responseEntity);

        //when
        String result = audioTranscriptionService.transcribe(s3Key);

        //then
        assertThat(result).isEqualTo(expectedText);
    }

    @DisplayName("S3 오디오 스트림 읽기 중 IOException이 발생하면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenIOExceptionOccursDuringStreaming() throws IOException {
        //given
        given(fileStorageService.streamAudio(s3Key)).willThrow(new IOException("S3 스트림 강제 예외 발생"));

        //when, then
        assertThatThrownBy(() -> audioTranscriptionService.transcribe(s3Key))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.WHISPER_API_FAILED.getMessage());
    }

}
