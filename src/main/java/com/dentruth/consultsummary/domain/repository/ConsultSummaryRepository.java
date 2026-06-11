package com.dentruth.consultsummary.domain.repository;

import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultSummaryRepository extends JpaRepository<ConsultSummary, UUID> {

    @Query("""
            SELECT c FROM ConsultSummary c
            WHERE c.userId = :userId AND c.isDeleted = false
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<ConsultSummary> findFirstPage(@Param("userId") UUID userId, PageRequest pageRequest);

    @Query("""
            SELECT c FROM ConsultSummary c
            WHERE c.userId = :userId AND c.isDeleted = false AND  (c.createdAt < :cursorCreatedAt OR (c.createdAt = :cursorCreatedAt AND c.id < :cursorId))
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<ConsultSummary> findNextPage(@Param("userId") UUID userId,
                                      @Param("cursorCreatedAt") Instant cursorCreatedAt,
                                      UUID cursorId, PageRequest pageRequest);

    List<ConsultSummary> findAllByStatus(SummaryStatus summaryStatus);

}
