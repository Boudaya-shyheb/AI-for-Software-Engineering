package com.aicareeragent.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.aicareeragent.dto.CodeDebugRequest;
import com.aicareeragent.service.AiServiceClient;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class CodeDebugWorkflowTest {
    @Test void sendsSelectedModuleAndCodeToTheExistingAiService() {
        CodeDebugRequest request = new CodeDebugRequest("profile", "python", "api_key = 'abcd1234'", "Do not expose secrets");
        AtomicReference<CodeDebugRequest> forwarded = new AtomicReference<>();
        AiServiceClient ai = new AiServiceClient(RestClient.builder(), "http://localhost:8000") {
            @Override public Map<?, ?> debugCode(CodeDebugRequest value) {
                forwarded.set(value);
                return Map.of("bug_type", "security", "findings", java.util.List.of());
            }
        };

        var response = new CodeDebugController(ai).debug(request);

        assertEquals("security", response.get("bug_type"));
        assertEquals(request, forwarded.get());
    }
}
