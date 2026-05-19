package com.dentruth.common.exception;

import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.code.ErrorStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtAuthenticationException(JwtAuthenticationException e) {
        log.warn("Jwt 인증에 실패했습니다. ErrorCode : {}, Message : {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.onFailure(e.getErrorCode(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, errors));
    }

    @ExceptionHandler(DentruthException.class)
    public ResponseEntity<?> handleDentruthException(DentruthException e) {
        ErrorStatus errorStatus = e.getErrorStatus();
        return ResponseEntity.status(errorStatus.getHttpStatus())
                .body(ApiResponse.onFailure(errorStatus, null));
    }

}
