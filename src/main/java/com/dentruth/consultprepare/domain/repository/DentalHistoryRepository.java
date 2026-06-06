package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.DentalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DentalHistoryRepository
        extends JpaRepository<DentalHistory, Long> {

    Optional<DentalHistory> findByName(String name);
}
