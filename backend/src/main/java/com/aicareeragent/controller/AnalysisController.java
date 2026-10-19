package com.aicareeragent.controller;

import com.aicareeragent.dto.AnalysisRequest;
import com.aicareeragent.service.AiServiceClient;
import com.aicareeragent.model.AnalysisRecord;
import com.aicareeragent.repository.AnalysisRecordRepository;
import com.aicareeragent.repository.UserRepository;
import com.aicareeragent.repository.JobOfferRepository;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalysisController {
    private final AiServiceClient aiServiceClient;
    private final UserRepository users;
    private final AnalysisRecordRepository analyses;
    private final JobOfferRepository offers;

    public AnalysisController(AiServiceClient aiServiceClient, UserRepository users, AnalysisRecordRepository analyses, JobOfferRepository offers) {
        this.aiServiceClient = aiServiceClient;
        this.users = users;
        this.analyses = analyses;
        this.offers = offers;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> analyze(@Valid @RequestBody AnalysisRequest request, Authentication authentication) {
        Map<?, ?> rawResponse = aiServiceClient.analyze(request);
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        rawResponse.forEach((key, value) -> response.put(String.valueOf(key), value));

        Object result = rawResponse.get("result");
        if (result instanceof Map<?, ?> resultMap && resultMap.get("compatibility_score") instanceof Number score) {
            users.findByEmailIgnoreCase(authentication.getName()).ifPresent(user -> {
                var offer = request.job_offer_id() == null ? null : offers.findById(request.job_offer_id()).orElse(null);
                AnalysisRecord record = analyses.save(new AnalysisRecord(user, offer, score.doubleValue()));
                if (record != null && record.getId() != null) {
                    response.put("analysis_id", record.getId());
                }
            });
        }
        return ResponseEntity.ok(response);
    }
}
