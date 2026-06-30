package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultMedicalHistoryRepository
        extends JpaRepository<ConsultMedicalHistory, Long> {

    List<ConsultMedicalHistory> findAllByConsultPrepareId(
            Long consultPrepareId
    );

    void deleteAllByConsultPrepareId(
            Long consultPrepareId
    );

    @Query("""
            select mh.nameKo
            from ConsultMedicalHistory cmh
            join MedicalHistory mh
                on cmh.medicalHistoryId = mh.id
            where cmh.consultPrepareId = :consultPrepareId
            """)
    List<String> findMedicalHistoryNames(
            @Param("consultPrepareId") Long consultPrepareId
    );

}
