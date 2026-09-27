package com.aicareeragent.service;

import com.aicareeragent.dto.AnalysisRequest;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiServiceClient {
    private final RestClient client;

    public AiServiceClient(RestClient.Builder builder, @Value("${ai.service-url:http://localhost:8000}") String serviceUrl) {
        this.client = builder
                .baseUrl(serviceUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    public Map<?, ?> analyze(AnalysisRequest request) {
        Map<String, String> payload = Map.of(
                "cv_text", request.cv_text(),
                "job_description", request.job_description());
        return client.post()
                .uri("/api/analyses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public Map<?, ?> extractDocument(MultipartFile file) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        return client.post()
                .uri("/api/documents/extract")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);
    }
}
