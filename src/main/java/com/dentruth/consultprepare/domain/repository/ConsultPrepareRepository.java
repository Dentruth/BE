package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultPrepareRepository
        extends JpaRepository<ConsultPrepare, Long> {

    List<ConsultPrepare> findAllByUserIdOrderByAppointmentDateDesc(
            String userId
    );

}
