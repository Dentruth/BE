package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalHistoryRepository
        extends JpaRepository<MedicalHistory, Long> {

    Optional<MedicalHistory> findByName(String name);
}
