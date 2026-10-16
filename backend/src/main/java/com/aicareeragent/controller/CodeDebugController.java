package com.aicareeragent.controller;

import com.aicareeragent.dto.CodeDebugRequest;
import com.aicareeragent.service.AiServiceClient;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code-debug")
public class CodeDebugController {
    private final AiServiceClient ai;

    public CodeDebugController(AiServiceClient ai) { this.ai = ai; }

    @PostMapping
    public Map<?, ?> debug(@Valid @RequestBody CodeDebugRequest request) {
        return ai.debugCode(request);
    }
}
