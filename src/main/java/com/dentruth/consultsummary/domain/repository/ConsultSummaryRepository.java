package com.dentruth.consultsummary.domain.repository;

import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultSummaryRepository extends JpaRepository<ConsultSummary, UUID> {
}
