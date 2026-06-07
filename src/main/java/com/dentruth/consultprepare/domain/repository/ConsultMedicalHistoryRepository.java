package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultMedicalHistoryRepository
        extends JpaRepository<ConsultMedicalHistory, Long> {

    List<ConsultMedicalHistory> findAllByConsultPrepareId(
            Long consultPrepareId
    );

    void deleteAllByConsultPrepareId(
            Long consultPrepareId
    );

}
