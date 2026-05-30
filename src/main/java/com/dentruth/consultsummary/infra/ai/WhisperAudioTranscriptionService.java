package com.dentruth.consultsummary.infra.ai;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.AudioTranscriptionService;
import com.dentruth.consultsummary.application.FileStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhisperAudioTranscriptionService implements AudioTranscriptionService {

    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.audio-model}")
    private String audioModel;

    @Value("${openai.whisper-url}")
    private String whisperUrl;

    @Override
    public String transcribe(String s3Key) {
        try (InputStream audioStream = fileStorageService.streamAudio(s3Key)) {
            String filename = extractFilename(s3Key);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new InputStreamResource(audioStream) {
                @Override
                public String getFilename() {
                    return filename;
                }

                @Override
                public long contentLength() {
                    return -1L;
                }
            });
            body.add("model", audioModel);
            body.add("language", "ko");
            body.add("temperature", "0.2");
            body.add("prompt", "이 내용은 치과 의사와 환자의 진료 내용입니다.");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    whisperUrl, requestEntity, Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody == null || !StringUtils.hasText((String) responseBody.get("text"))) {
                log.error("Whisper 응답에 text가 없습니다. S3Key : [{}]", s3Key);
                throw new DentruthException(ErrorStatus.WHISPER_API_FAILED);
            }

            String result = (String) responseBody.get("text");
            log.info("Whisper STT 완료. S3Key : [{}]", s3Key);
            return result;
        } catch (NoSuchKeyException e) {
            log.error("S3 오디오 파일을 찾을 수 없습니다. S3Key : [{}]", s3Key, e);
            throw new DentruthException(ErrorStatus.AUDIO_FILE_NOT_FOUND);

        } catch (IOException e) {
            log.error("Whisper STT 실패. S3Key : [{}]", s3Key, e);
            throw new DentruthException(ErrorStatus.WHISPER_API_FAILED);
        } catch (RestClientException e) {
            log.error("Whisper API 호출 실패. S3Key : [{}]", s3Key, e);
            throw new DentruthException(ErrorStatus.WHISPER_API_FAILED);
        } catch (DentruthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Whisper STT 알 수 없는 실패. S3Key : [{}]", s3Key, e);
            throw new DentruthException(ErrorStatus.WHISPER_API_FAILED);
        }
    }

    private String extractFilename(String s3Key) {
        return s3Key.substring(s3Key.lastIndexOf("/") + 1);
    }

}
