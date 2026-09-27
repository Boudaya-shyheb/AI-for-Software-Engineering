package com.aicareeragent.controller;

import com.aicareeragent.dto.UserSummary;
import com.aicareeragent.repository.AnalysisRecordRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.UserRepository;
import com.aicareeragent.repository.JobApplicationRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final AnalysisRecordRepository analyses;
    private final JobOfferRepository offers;
    private final JobApplicationRepository applications;

    public AdminController(UserRepository users, AnalysisRecordRepository analyses, JobOfferRepository offers, JobApplicationRepository applications) {
        this.users = users;
        this.analyses = analyses;
        this.offers = offers;
        this.applications = applications;
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of("users", users.count(), "analyses", analyses.count(), "jobOffers", offers.count(), "applications", applications.count());
    }

    @GetMapping("/users")
    public List<UserSummary> users() {
        return users.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(UserSummary::from).toList();
    }
}
