package com.dentruth.consultsummary.domain.entity;

import com.dentruth.common.domain.BaseEntity;
import com.dentruth.common.util.EncryptedStringConverter;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "consult_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = {"diagnosis", "treatmentPlan", "diagnosticResult"})
@Getter
@Builder
@Slf4j
public class ConsultSummary extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String audioLink;

    private String title;
    private String clinicName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String diagnosticResult;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SummaryStatus status;

    private String failReason;

    private Boolean isDeleted;

    private String practitionerName;
    private String licenseType;
    private String institution;

    @Convert(converter = EncryptedStringConverter.class)
    private String licenseNumber;


    public static ConsultSummary create(UUID userId, String audioLink, String clinicName) {
        return ConsultSummary.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .audioLink(audioLink)
                .clinicName(clinicName)
                .status(SummaryStatus.PENDING)
                .isDeleted(false)
                .build();
    }

    public static ConsultSummary create(UUID userId, String audioLink, String clinicName, String practitionerName,
                                        String licenseType, String licenseNumber, String institution) {
        return ConsultSummary.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .audioLink(audioLink)
                .clinicName(clinicName)
                .status(SummaryStatus.PENDING)
                .isDeleted(false)
                .licenseType(licenseType)
                .licenseNumber(licenseNumber)
                .practitionerName(practitionerName)
                .institution(institution)
                .build();
    }

    public void markAsCompleted(String diagnosticResult, String diagnosis, String title) {
        this.status = SummaryStatus.COMPLETED;
        this.diagnosis = diagnosis;
        this.title = title;
        this.diagnosticResult = diagnosticResult;
    }

    public void markAsFailed(String failReason) {
        this.status = SummaryStatus.FAILED;
        this.failReason = failReason;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void updateSummary(String clinicName, String shortDiagnosis, String jsonDiagnosticResult) {
        if (clinicName != null) {
            this.clinicName = clinicName;
        }
        if (shortDiagnosis != null) {
            this.diagnosis = shortDiagnosis;
            this.title = shortDiagnosis;
        }
        if (jsonDiagnosticResult != null) {
            this.diagnosticResult = jsonDiagnosticResult;
        }
    }

}
