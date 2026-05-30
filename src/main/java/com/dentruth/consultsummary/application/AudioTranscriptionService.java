package com.dentruth.consultsummary.application;

public interface AudioTranscriptionService {
    String transcribe(String s3Key);
}
