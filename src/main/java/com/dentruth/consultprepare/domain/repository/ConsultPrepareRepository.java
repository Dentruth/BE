package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultPrepareRepository
        extends JpaRepository<ConsultPrepare, Long> {
}
