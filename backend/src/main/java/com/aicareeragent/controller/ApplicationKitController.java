package com.aicareeragent.controller;

import com.aicareeragent.dto.ApplicationKitCreateRequest;
import com.aicareeragent.dto.ApplicationKitResponse;
import com.aicareeragent.dto.CoverLetterUpdateRequest;
import com.aicareeragent.service.ApplicationKitService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/application-kits")
public class ApplicationKitController {
    private final ApplicationKitService applicationKits;

    public ApplicationKitController(ApplicationKitService applicationKits) {
        this.applicationKits = applicationKits;
    }

    /** Creates an application kit for the authenticated user. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationKitResponse create(@Valid @RequestBody ApplicationKitCreateRequest request,
            Authentication auth) {
        return applicationKits.generate(auth.getName(), request);
    }

    /** Lists application kits owned by the authenticated user. */
    @GetMapping
    public List<ApplicationKitResponse> list(Authentication auth) {
        return applicationKits.list(auth.getName());
    }

    /** Retrieves an application kit owned by the authenticated user. */
    @GetMapping("/{id}")
    public ApplicationKitResponse get(@PathVariable Long id, Authentication auth) {
        return applicationKits.get(auth.getName(), id);
    }

    /** Updates the cover letter of an application kit owned by the authenticated user. */
    @PutMapping("/{id}/cover-letter")
    public ApplicationKitResponse updateCoverLetter(@PathVariable Long id,
            @Valid @RequestBody CoverLetterUpdateRequest request, Authentication auth) {
        return applicationKits.updateCoverLetter(auth.getName(), id, request.coverLetter());
    }

    /** Deletes an application kit owned by the authenticated user. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        applicationKits.delete(auth.getName(), id);
    }
}
