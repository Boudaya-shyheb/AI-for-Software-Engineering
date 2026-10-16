package com.aicareeragent.repository;

import com.aicareeragent.model.AnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, Long> {
    @Query("select analysis from AnalysisRecord analysis left join fetch analysis.jobOffer "
            + "where lower(analysis.user.email) = lower(:email) order by analysis.createdAt desc")
    List<AnalysisRecord> findHistoryByUserEmail(@Param("email") String email);
}
