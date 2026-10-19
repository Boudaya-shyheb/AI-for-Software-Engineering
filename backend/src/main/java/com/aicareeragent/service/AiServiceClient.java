package com.aicareeragent.service;

import com.aicareeragent.dto.AnalysisRequest;
import com.aicareeragent.dto.CodeDebugRequest;
import java.io.IOException;
import java.util.Map;
import java.util.List;
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

    public Map<?, ?> matchJobs(String cvText, List<Map<String, Object>> offers) {
        return client.post()
                .uri("/api/job-matches")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("cv_text", cvText, "offers", offers))
                .retrieve()
                .body(Map.class);
    }

    public Map<?, ?> generateInterview(String cvText, String jobDescription, int questionCount) {
        return client.post().uri("/api/interviews/generate").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("cv_text", cvText, "job_description", jobDescription, "question_count", questionCount))
                .retrieve().body(Map.class);
    }

    /** Generates an application kit from a CV and a job offer. */
    public Map<?, ?> generateApplication(String cvText, String jobTitle, String company,
            String jobDescription, String candidateName) {
        return client.post().uri("/api/applications/generate").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "cv_text", cvText,
                        "job_title", jobTitle,
                        "company", company,
                        "job_description", jobDescription,
                        "candidate_name", candidateName))
                .retrieve().body(Map.class);
    }

    public Map<?, ?> evaluateInterviewAnswer(String question, String answer, String cvText, String jobDescription, String type) {
        return client.post().uri("/api/interviews/evaluate").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("question", question, "answer", answer, "cv_text", cvText, "job_description", jobDescription, "question_type", type))
                .retrieve().body(Map.class);
    }

    public Map<?, ?> summarizeInterview(java.util.List<?> evaluations) {
        return client.post().uri("/api/interviews/summary").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("evaluations", evaluations)).retrieve().body(Map.class);
    }

    public Map<?, ?> debugCode(CodeDebugRequest request) {
        return client.post().uri("/api/code-debug").contentType(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(Map.class);
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
