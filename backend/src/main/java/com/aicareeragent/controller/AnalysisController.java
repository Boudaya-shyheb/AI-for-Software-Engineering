package com.aicareeragent.controller;

import com.aicareeragent.dto.AnalysisRequest;
import com.aicareeragent.service.AiServiceClient;
import com.aicareeragent.model.AnalysisRecord;
import com.aicareeragent.repository.AnalysisRecordRepository;
import com.aicareeragent.repository.UserRepository;
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

    public AnalysisController(AiServiceClient aiServiceClient, UserRepository users, AnalysisRecordRepository analyses) {
        this.aiServiceClient = aiServiceClient;
        this.users = users;
        this.analyses = analyses;
    }

    @PostMapping
    public ResponseEntity<Map<?, ?>> analyze(@Valid @RequestBody AnalysisRequest request, Authentication authentication) {
        Map<?, ?> response = aiServiceClient.analyze(request);
        Object result = response.get("result");
        if (result instanceof Map<?, ?> resultMap && resultMap.get("compatibility_score") instanceof Number score) {
            users.findByEmailIgnoreCase(authentication.getName())
                    .ifPresent(user -> analyses.save(new AnalysisRecord(user, score.doubleValue())));
        }
        return ResponseEntity.ok(response);
    }
}
