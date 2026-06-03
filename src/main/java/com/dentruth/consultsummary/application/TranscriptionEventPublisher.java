package com.dentruth.consultsummary.application;

import java.util.UUID;

public interface TranscriptionEventPublisher {
    void publish(UUID summaryId, String s3Key, String clinicName, String createdAt);
}
