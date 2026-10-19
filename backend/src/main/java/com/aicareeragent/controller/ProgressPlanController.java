package com.aicareeragent.controller;

import com.aicareeragent.dto.GoalStatusUpdateRequest;
import com.aicareeragent.dto.ProgressPlanCreateRequest;
import com.aicareeragent.dto.ProgressPlanResponse;
import com.aicareeragent.model.AnalysisRecord;
import com.aicareeragent.model.ProgressGoal;
import com.aicareeragent.model.ProgressPlan;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.AnalysisRecordRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.ProgressPlanRepository;
import com.aicareeragent.repository.UserRepository;
import com.aicareeragent.service.ProgressPlanService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/progress-plans")
public class ProgressPlanController {
    private final ProgressPlanRepository plans;
    private final UserRepository users;
    private final AnalysisRecordRepository analyses;
    private final JobOfferRepository offers;
    private final ProgressPlanService progressPlanService;

    public ProgressPlanController(ProgressPlanRepository plans, UserRepository users,
            AnalysisRecordRepository analyses, JobOfferRepository offers, ProgressPlanService progressPlanService) {
        this.plans = plans;
        this.users = users;
        this.analyses = analyses;
        this.offers = offers;
        this.progressPlanService = progressPlanService;
    }

    @GetMapping
    public List<ProgressPlanResponse> list(Authentication auth) {
        return plans.findByUserEmailOrderByCreatedAtDesc(auth.getName()).stream()
                .map(ProgressPlanResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProgressPlanResponse get(@PathVariable Long id, Authentication auth) {
        return ProgressPlanResponse.from(findOwnedPlan(id, auth.getName()));
    }

    @PostMapping
    public ResponseEntity<ProgressPlanResponse> create(@Valid @RequestBody ProgressPlanCreateRequest request, Authentication auth) {
        var user = findUser(auth);
        var normalizedSkills = request.normalizedSkills();

        AnalysisRecord analysis = null;
        if (request.analysisId() != null) {
            analysis = analyses.findById(request.analysisId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));
            if (!analysis.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this analysis");
            }
            var existing = plans.findByUserIdAndAnalysisId(user.getId(), analysis.getId());
            if (!existing.isEmpty()) {
                return ResponseEntity.ok(ProgressPlanResponse.from(existing.getFirst()));
            }
        }

        var offer = request.jobOfferId() == null ? null : offers.findById(request.jobOfferId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job offer not found"));

        var plan = progressPlanService.buildPlan(user, analysis, offer, normalizedSkills);
        if (plan.getGoals().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing skills are required to build a progression plan");
        }
        ProgressPlan saved = plans.save(plan);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProgressPlanResponse.from(saved));
    }

    @PatchMapping("/{planId}/goals/{goalId}")
    public ProgressPlanResponse updateGoalStatus(@PathVariable Long planId, @PathVariable Long goalId,
            @Valid @RequestBody GoalStatusUpdateRequest request, Authentication auth) {
        ProgressPlan plan = findOwnedPlan(planId, auth.getName());
        ProgressGoal goal = plan.getGoals().stream()
                .filter(candidate -> candidate.getId() != null && candidate.getId().equals(goalId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found in this plan"));

        goal.updateStatus(request.status());
        plans.save(plan);
        return ProgressPlanResponse.from(plan);
    }

    private UserAccount findUser(Authentication auth) {
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private ProgressPlan findOwnedPlan(Long planId, String email) {
        return plans.findByIdAndUserEmail(planId, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Progress plan not found"));
    }
}
