package com.aicareeragent.service;

import com.aicareeragent.dto.AnalysisRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiServiceClient {
    private final RestClient client;

    public AiServiceClient(RestClient.Builder builder, @Value("${ai.service-url:http://localhost:8000}") String serviceUrl) {
        this.client = builder.baseUrl(serviceUrl).build();
    }

    public Map<?, ?> analyze(AnalysisRequest request) {
        return client.post().uri("/api/analyses").body(request).retrieve().body(Map.class);
    }
}
