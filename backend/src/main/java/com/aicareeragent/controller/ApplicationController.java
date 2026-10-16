package com.aicareeragent.controller;

import com.aicareeragent.dto.ApplicationRequest;
import com.aicareeragent.dto.ApplicationResponse;
import com.aicareeragent.model.ApplicationStatus;
import com.aicareeragent.model.JobApplication;
import com.aicareeragent.repository.JobApplicationRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/applications")
public class ApplicationController {
    private final JobApplicationRepository applications; private final JobOfferRepository offers; private final UserRepository users;
    public ApplicationController(JobApplicationRepository applications, JobOfferRepository offers, UserRepository users) { this.applications=applications; this.offers=offers; this.users=users; }
    @GetMapping public List<ApplicationResponse> list(Authentication auth) { return applications.findForUser(auth.getName()).stream().map(ApplicationResponse::from).toList(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@Valid @RequestBody ApplicationRequest request, Authentication auth) {
        var user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        var offer=offers.findById(request.jobOfferId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Job offer not found"));
        if (applications.existsByUserIdAndJobOfferId(user.getId(),offer.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT,"You already applied to this offer");
        if (user.getSavedCvText().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Save a CV in your profile before applying");
        return ApplicationResponse.from(applications.save(new JobApplication(user,offer,request.coverLetter(),user.getSavedCvText())));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable Long id, Authentication auth) {
        var application=applications.findDetailedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found"));
        if (!application.getUser().getEmail().equalsIgnoreCase(auth.getName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your application");
        application.changeStatus(ApplicationStatus.WITHDRAWN); applications.save(application);
    }
}
