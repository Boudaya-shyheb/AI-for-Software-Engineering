package com.aicareeragent.controller;

import com.aicareeragent.dto.JobOfferRequest;
import com.aicareeragent.dto.JobOfferResponse;
import com.aicareeragent.model.JobOffer;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.JobApplicationRepository;
import com.aicareeragent.repository.FavoriteOfferRepository;
import com.aicareeragent.repository.ApplicationKitRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/job-offers")
public class AdminJobOfferController {
    private final JobOfferRepository offers;
    private final JobApplicationRepository applications;
    private final FavoriteOfferRepository favorites;
    private final ApplicationKitRepository applicationKits;

    public AdminJobOfferController(JobOfferRepository offers, JobApplicationRepository applications,
            FavoriteOfferRepository favorites, ApplicationKitRepository applicationKits) {
        this.offers = offers;
        this.applications = applications;
        this.favorites = favorites;
        this.applicationKits = applicationKits;
    }

    @GetMapping
    public List<JobOfferResponse> list() {
        return offers.findAllByOrderByCreatedAtDesc().stream().map(JobOfferResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobOfferResponse create(@Valid @RequestBody JobOfferRequest request) {
        JobOffer offer = new JobOffer(request.title(), request.company(), request.location(),
                request.employmentType(), request.description());
        return JobOfferResponse.from(offers.save(offer));
    }

    @PutMapping("/{id}")
    public JobOfferResponse update(@PathVariable Long id, @Valid @RequestBody JobOfferRequest request) {
        JobOffer offer = find(id);
        offer.update(request.title(), request.company(), request.location(), request.employmentType(), request.description());
        return JobOfferResponse.from(offers.save(offer));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (applications.existsByJobOfferId(id)
                || favorites.existsByJobOfferId(id)
                || applicationKits.existsByJobOfferId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This offer still has linked records. Archive it instead.");
        }
        offers.delete(find(id));
    }

    @PatchMapping("/{id}/active")
    public JobOfferResponse active(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> body) {
        JobOffer offer=find(id); offer.setActive(Boolean.TRUE.equals(body.get("active")));
        return JobOfferResponse.from(offers.save(offer));
    }

    private JobOffer find(Long id) {
        return offers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job offer not found"));
    }
}
