package com.aicareeragent.controller;

import com.aicareeragent.dto.AnalysisHistoryResponse;
import com.aicareeragent.repository.AnalysisRecordRepository;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class AnalysisHistoryController {
    private final AnalysisRecordRepository analyses;
    public AnalysisHistoryController(AnalysisRecordRepository analyses) { this.analyses = analyses; }

    @GetMapping
    public List<AnalysisHistoryResponse> list(Authentication auth) {
        return analyses.findHistoryByUserEmail(auth.getName()).stream()
                .map(AnalysisHistoryResponse::from).toList();
    }
}
