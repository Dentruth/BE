package com.dentruth.consultprepare.domain.entity;

import com.dentruth.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultDentalHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long consultPrepareId;

    private Long dentalHistoryId;

    public ConsultDentalHistory(
            Long consultPrepareId,
            Long dentalHistoryId
    ) {
        this.consultPrepareId = consultPrepareId;
        this.dentalHistoryId = dentalHistoryId;
    }

}
