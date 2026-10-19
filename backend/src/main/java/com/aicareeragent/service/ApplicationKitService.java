package com.aicareeragent.service;

import com.aicareeragent.dto.ApplicationKitCreateRequest;
import com.aicareeragent.dto.ApplicationKitResponse;
import com.aicareeragent.model.ApplicationKit;
import com.aicareeragent.model.JobOffer;
import com.aicareeragent.model.Role;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.ApplicationKitRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationKitService {
    private static final String INVALID_AI_RESPONSE = "The AI service returned an invalid application";

    private final ApplicationKitRepository kits;
    private final JobOfferRepository offers;
    private final UserRepository users;
    private final AiServiceClient ai;

    public ApplicationKitService(ApplicationKitRepository kits, JobOfferRepository offers,
            UserRepository users, AiServiceClient ai) {
        this.kits = kits;
        this.offers = offers;
        this.users = users;
        this.ai = ai;
    }

    /** Generates and stores an application kit for an active offer. */
    @Transactional
    public ApplicationKitResponse generate(String email, ApplicationKitCreateRequest request) {
        UserAccount user = findUser(email);
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrator accounts cannot generate applications");
        }
        JobOffer offer = offers.findById(request.jobOfferId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job offer not found"));
        if (!offer.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This job offer is archived");
        }
        String cvText = hasText(request.cvText()) ? request.cvText().trim() : user.getSavedCvText();
        if (!hasText(cvText)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Add a CV to your profile or paste one");
        }

        Map<?, ?> generated = ai.generateApplication(
                cvText, offer.getTitle(), offer.getCompany(), offer.getDescription(), user.getFullName());
        GeneratedApplication application = readGeneratedApplication(generated);
        if (!application.verified()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Generated documents failed the no-invention check");
        }
        ApplicationKit kit = kits.save(new ApplicationKit(user, offer, application.adaptedCv(),
                application.coverLetter(), application.matchedSkills(), application.missingSkills()));
        return ApplicationKitResponse.from(kit);
    }

    /** Lists the authenticated user's application kits. */
    @Transactional(readOnly = true)
    public List<ApplicationKitResponse> list(String email) {
        findUser(email);
        return kits.findAllByOwnerEmail(email).stream()
                .map(ApplicationKitResponse::from)
                .toList();
    }

    /** Retrieves one application kit owned by the user. */
    @Transactional(readOnly = true)
    public ApplicationKitResponse get(String email, Long id) {
        findUser(email);
        return ApplicationKitResponse.from(owned(id, email));
    }

    /** Updates the cover letter of an application kit owned by the user. */
    @Transactional
    public ApplicationKitResponse updateCoverLetter(String email, Long id, String coverLetter) {
        findUser(email);
        ApplicationKit kit = owned(id, email);
        kit.updateCoverLetter(coverLetter);
        return ApplicationKitResponse.from(kit);
    }

    /** Deletes an application kit owned by the user. */
    @Transactional
    public void delete(String email, Long id) {
        findUser(email);
        kits.delete(owned(id, email));
    }

    private UserAccount findUser(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private ApplicationKit owned(Long id, String email) {
        return kits.findByIdAndOwnerEmail(id, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application kit not found"));
    }

    private GeneratedApplication readGeneratedApplication(Map<?, ?> generated) {
        if (generated == null) {
            throw invalidAiResponse();
        }
        String adaptedCv = requiredString(generated, "adapted_cv");
        String coverLetter = requiredString(generated, "cover_letter");
        List<String> matchedSkills = requiredStringList(generated, "matched_skills");
        List<String> missingSkills = requiredStringList(generated, "missing_skills");
        Object verificationValue = generated.get("verification");
        if (!(verificationValue instanceof Map<?, ?> verification)) {
            throw invalidAiResponse();
        }
        Object verifiedValue = verification.get("verified");
        if (!(verifiedValue instanceof Boolean verified)) {
            throw invalidAiResponse();
        }
        requiredStringList(verification, "unsupported_skills");
        return new GeneratedApplication(adaptedCv, coverLetter, matchedSkills, missingSkills, verified);
    }

    private String requiredString(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw invalidAiResponse();
        }
        return text;
    }

    private List<String> requiredStringList(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?> items)
                || items.stream().anyMatch(item -> !(item instanceof String))) {
            throw invalidAiResponse();
        }
        return items.stream().map(item -> (String) item).toList();
    }

    private ResponseStatusException invalidAiResponse() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, INVALID_AI_RESPONSE);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record GeneratedApplication(String adaptedCv, String coverLetter,
            List<String> matchedSkills, List<String> missingSkills, boolean verified) {}
}
