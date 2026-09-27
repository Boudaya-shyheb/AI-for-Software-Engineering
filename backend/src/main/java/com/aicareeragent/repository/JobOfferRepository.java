package com.aicareeragent.repository;

import com.aicareeragent.model.JobOffer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {
    List<JobOffer> findAllByOrderByCreatedAtDesc();
}
