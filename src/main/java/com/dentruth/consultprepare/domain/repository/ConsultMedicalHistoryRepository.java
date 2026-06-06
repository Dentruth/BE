package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultMedicalHistoryRepository
        extends JpaRepository<ConsultMedicalHistory, Long> {
}
