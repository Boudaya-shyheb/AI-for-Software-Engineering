package com.aicareeragent.controller;

import com.aicareeragent.dto.JobOfferResponse;
import com.aicareeragent.dto.JobMatchRequest;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.service.AiServiceClient;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/job-offers")
public class JobOfferController {
    private final JobOfferRepository offers;
    private final AiServiceClient ai;

    public JobOfferController(JobOfferRepository offers, AiServiceClient ai) { this.offers = offers; this.ai = ai; }

    @GetMapping
    public List<JobOfferResponse> list() {
        return offers.findByActiveTrueOrderByCreatedAtDesc().stream().map(JobOfferResponse::from).toList();
    }

    @PostMapping("/matches")
    public Map<?, ?> matches(@Valid @RequestBody JobMatchRequest request) {
        List<Map<String, Object>> catalog = offers.findByActiveTrueOrderByCreatedAtDesc().stream().map(offer -> Map.<String, Object>of(
                "id", offer.getId(),
                "title", offer.getTitle(),
                "company", offer.getCompany(),
                "location", offer.getLocation(),
                "employmentType", offer.getEmploymentType(),
                "description", offer.getDescription())).toList();
        return ai.matchJobs(request.cvText(), catalog);
    }

    @GetMapping("/{id}")
    public JobOfferResponse get(@PathVariable Long id) {
        return offers.findById(id).filter(com.aicareeragent.model.JobOffer::isActive).map(JobOfferResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job offer not found"));
    }
}
