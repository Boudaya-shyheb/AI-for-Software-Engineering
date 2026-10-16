package com.aicareeragent.repository;

import com.aicareeragent.model.JobApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    boolean existsByUserIdAndJobOfferId(Long userId, Long jobOfferId);
    boolean existsByJobOfferId(Long jobOfferId);
    @Query("select application from JobApplication application join fetch application.jobOffer join fetch application.user where lower(application.user.email)=lower(:email) order by application.createdAt desc")
    List<JobApplication> findForUser(@Param("email") String email);
    @Query("select application from JobApplication application join fetch application.jobOffer join fetch application.user order by application.createdAt desc")
    List<JobApplication> findAllDetailed();
    @Query("select application from JobApplication application join fetch application.jobOffer join fetch application.user where application.id=:id")
    Optional<JobApplication> findDetailedById(@Param("id") Long id);
}
