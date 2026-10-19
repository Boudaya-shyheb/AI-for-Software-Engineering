package com.aicareeragent.repository;

import com.aicareeragent.model.ProgressPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgressPlanRepository extends JpaRepository<ProgressPlan, Long> {
    @Query("select plan from ProgressPlan plan left join fetch plan.goals where lower(plan.user.email) = lower(:email) order by plan.createdAt desc")
    List<ProgressPlan> findByUserEmailOrderByCreatedAtDesc(@Param("email") String email);

    @Query("select plan from ProgressPlan plan left join fetch plan.goals where plan.id = :id and lower(plan.user.email) = lower(:email)")
    Optional<ProgressPlan> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);

    @Query("select plan from ProgressPlan plan left join fetch plan.goals where plan.user.id = :userId and plan.analysis.id = :analysisId order by plan.createdAt desc")
    List<ProgressPlan> findByUserIdAndAnalysisId(@Param("userId") Long userId, @Param("analysisId") Long analysisId);

    @Query("select plan from ProgressPlan plan left join fetch plan.goals where plan.user.id = :userId and plan.jobOffer.id = :jobOfferId order by plan.createdAt desc")
    List<ProgressPlan> findByUserIdAndJobOfferId(@Param("userId") Long userId, @Param("jobOfferId") Long jobOfferId);
}
