package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultPrepareRepository
        extends JpaRepository<ConsultPrepare, Long> {

    List<ConsultPrepare> findAllByUserIdOrderByAppointmentDateDesc(
            UUID userId
    );

    List<ConsultPrepare> findAllByUserId(UUID userId);


}
