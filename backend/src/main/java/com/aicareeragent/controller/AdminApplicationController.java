package com.aicareeragent.controller;

import com.aicareeragent.dto.ApplicationResponse;
import com.aicareeragent.dto.ApplicationStatusRequest;
import com.aicareeragent.repository.JobApplicationRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/admin/applications")
public class AdminApplicationController {
    private final JobApplicationRepository applications;
    public AdminApplicationController(JobApplicationRepository applications) { this.applications=applications; }
    @GetMapping public List<ApplicationResponse> list() { return applications.findAllDetailed().stream().map(ApplicationResponse::from).toList(); }
    @PatchMapping("/{id}/status") public ApplicationResponse status(@PathVariable Long id, @Valid @RequestBody ApplicationStatusRequest request) {
        var application=applications.findDetailedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found"));
        application.changeStatus(request.status());
        applications.save(application);
        return ApplicationResponse.from(application);
    }
}
