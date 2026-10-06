package com.aicareeragent.controller;

import com.aicareeragent.dto.InterviewAnswerRequest;
import com.aicareeragent.dto.InterviewCreateRequest;
import com.aicareeragent.service.InterviewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {
    private final InterviewService interviews;
    public InterviewController(InterviewService interviews) { this.interviews = interviews; }
    @PostMapping public Map<String, Object> create(@Valid @RequestBody InterviewCreateRequest request, Authentication auth) { return interviews.create(auth.getName(), request); }
    @GetMapping public List<Map<String, Object>> list(Authentication auth) { return interviews.list(auth.getName()); }
    @GetMapping("/{id}") public Map<String, Object> get(@PathVariable Long id, Authentication auth) { return interviews.get(auth.getName(), id); }
    @PostMapping("/{id}/answers") public Map<String, Object> answer(@PathVariable Long id, @Valid @RequestBody InterviewAnswerRequest request, Authentication auth) { return interviews.answer(auth.getName(), id, request); }
}
