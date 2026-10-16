package com.aicareeragent.controller;

import com.aicareeragent.dto.UserSummary;
import com.aicareeragent.dto.AdminInterviewResult;
import com.aicareeragent.service.AdminInterviewService;
import com.aicareeragent.repository.AnalysisRecordRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.UserRepository;
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
    private final AdminInterviewService interviews;

    public AdminController(UserRepository users, AnalysisRecordRepository analyses, JobOfferRepository offers, AdminInterviewService interviews) {
        this.users = users;
        this.analyses = analyses;
        this.offers = offers;
        this.interviews = interviews;
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of("users", users.count(), "analyses", analyses.count(), "jobOffers", offers.count());
    }

    @GetMapping("/users")
    public List<UserSummary> users() {
        return users.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(UserSummary::from).toList();
    }

    @GetMapping("/interviews")
    public List<AdminInterviewResult> interviews() { return interviews.listAll(); }
}
