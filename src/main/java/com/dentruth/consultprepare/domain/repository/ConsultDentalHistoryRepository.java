package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultDentalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultDentalHistoryRepository
        extends JpaRepository<ConsultDentalHistory, Long> {
}
