package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultPrepareRepository
        extends JpaRepository<ConsultPrepare, Long> {

    Optional<ConsultPrepare> findByIdAndUserIdAndDeletedAtIsNull(
            Long id,
            UUID userId
    );

    List<ConsultPrepare>
    findAllByUserIdAndDeletedAtIsNullOrderByAppointmentDateDesc(
            UUID userId
    );

    List<ConsultPrepare> findAllByUserIdAndDeletedAtIsNullAndAppointmentDateBetween(
            UUID userId,
            LocalDateTime start,
            LocalDateTime end
    );

}
