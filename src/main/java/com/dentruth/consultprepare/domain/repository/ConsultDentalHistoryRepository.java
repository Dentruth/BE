package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultDentalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultDentalHistoryRepository
        extends JpaRepository<ConsultDentalHistory, Long> {

    List<ConsultDentalHistory> findAllByConsultPrepareId(
            Long consultPrepareId
    );

    void deleteAllByConsultPrepareId(
            Long consultPrepareId
    );

    @Query("""
            select dh.nameKo
            from ConsultDentalHistory cdh
            join DentalHistory dh
                on cdh.dentalHistoryId = dh.id
            where cdh.consultPrepareId = :consultPrepareId
            """)
    List<String> findDentalHistoryNames(
            @Param("consultPrepareId") Long consultPrepareId
    );

}
