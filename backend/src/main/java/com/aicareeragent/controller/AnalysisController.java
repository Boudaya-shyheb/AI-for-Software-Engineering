package com.aicareeragent.controller;

import com.aicareeragent.dto.AnalysisRequest;
import com.aicareeragent.service.AiServiceClient;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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

    public AnalysisController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @PostMapping
    public ResponseEntity<Map<?, ?>> analyze(@Valid @RequestBody AnalysisRequest request) {
        return ResponseEntity.ok(aiServiceClient.analyze(request));
    }
}
