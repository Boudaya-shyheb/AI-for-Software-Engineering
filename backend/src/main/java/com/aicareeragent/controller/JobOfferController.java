package com.aicareeragent.controller;

import com.aicareeragent.dto.JobOfferResponse;
import com.aicareeragent.repository.JobOfferRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/job-offers")
public class JobOfferController {
    private final JobOfferRepository offers;

    public JobOfferController(JobOfferRepository offers) { this.offers = offers; }

    @GetMapping
    public List<JobOfferResponse> list() {
        return offers.findByActiveTrueOrderByCreatedAtDesc().stream().map(JobOfferResponse::from).toList();
    }

    @GetMapping("/{id}")
    public JobOfferResponse get(@PathVariable Long id) {
        return offers.findById(id).filter(com.aicareeragent.model.JobOffer::isActive).map(JobOfferResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job offer not found"));
    }
}
