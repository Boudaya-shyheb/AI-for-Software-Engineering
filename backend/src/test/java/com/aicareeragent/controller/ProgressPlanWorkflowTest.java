package com.aicareeragent.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aicareeragent.dto.GoalStatusUpdateRequest;
import com.aicareeragent.dto.ProgressPlanCreateRequest;
import com.aicareeragent.model.AnalysisRecord;
import com.aicareeragent.model.JobOffer;
import com.aicareeragent.model.ProgressGoal;
import com.aicareeragent.model.ProgressPlan;
import com.aicareeragent.model.ProgressStatus;
import com.aicareeragent.model.Role;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.AnalysisRecordRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.ProgressPlanRepository;
import com.aicareeragent.repository.UserRepository;
import com.aicareeragent.service.ProgressPlanService;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class ProgressPlanWorkflowTest {
    @Test
    void creatingAProgressPlanFromTheOwnerAnalysisPersistsTheGeneratedGoals() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ProgressPlanRepository plans = mock(ProgressPlanRepository.class);
        AnalysisRecordRepository analyses = mock(AnalysisRecordRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserAccount owner = new UserAccount("Candidate", "candidate@example.test", "hash", Role.USER);
        setId(owner, 12L);
        JobOffer offer = new JobOffer("Backend engineer", "CloudCo", "Remote", "CDI", "Java Spring Boot");
        setId(offer, 9L);
        AnalysisRecord analysis = new AnalysisRecord(owner, offer, 0.88);
        setId(analysis, 7L);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("candidate@example.test");
        when(users.findByEmailIgnoreCase("candidate@example.test")).thenReturn(Optional.of(owner));
        when(analyses.findById(7L)).thenReturn(Optional.of(analysis));
        when(offers.findById(9L)).thenReturn(Optional.of(offer));
        when(plans.save(any(ProgressPlan.class))).thenAnswer(invocation -> {
            ProgressPlan saved = invocation.getArgument(0);
            setId(saved, 101L);
            return saved;
        });

        ProgressPlanController controller = new ProgressPlanController(plans, users, analyses, offers, new ProgressPlanService());

        var result = controller.create(new ProgressPlanCreateRequest(7L, 9L, "Plan Java", "Summary", List.of("Java", "Spring Boot")), auth);

        assertEquals(201, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().goals().size());
        assertEquals("Java", result.getBody().goals().getFirst().skillName());
        verify(plans).save(any(ProgressPlan.class));
    }

    @Test
    void emptyOrUnknownSkillsFallBackToAGenericProgressGoal() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ProgressPlanRepository plans = mock(ProgressPlanRepository.class);
        UserAccount owner = new UserAccount("Candidate", "candidate@example.test", "hash", Role.USER);
        setId(owner, 13L);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("candidate@example.test");
        when(users.findByEmailIgnoreCase("candidate@example.test")).thenReturn(Optional.of(owner));
        when(plans.save(any(ProgressPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressPlanController controller = new ProgressPlanController(plans, users, mock(AnalysisRecordRepository.class), mock(JobOfferRepository.class), new ProgressPlanService());

        var result = controller.create(new ProgressPlanCreateRequest(null, null, null, null, List.of("   ", "Unknown skill")), auth);

        assertEquals(201, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().goals().size());
        assertEquals("Unknown skill", result.getBody().goals().getFirst().skillName());
    }

    @Test
    void duplicateAnalysisCreatesOnlyOnePlanAndReusesTheStoredVersion() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ProgressPlanRepository plans = mock(ProgressPlanRepository.class);
        AnalysisRecordRepository analyses = mock(AnalysisRecordRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserAccount owner = new UserAccount("Candidate", "candidate@example.test", "hash", Role.USER);
        setId(owner, 14L);
        AnalysisRecord analysis = new AnalysisRecord(owner, null, 0.76);
        setId(analysis, 8L);
        ProgressPlan existingPlan = new ProgressPlan(owner, analysis, null, "Plan Java", "Summary");
        setId(existingPlan, 22L);
        ProgressGoal existingGoal = new ProgressGoal(existingPlan, "Java", "Objective", "Activity", "Exercise", 0);
        setId(existingGoal, 31L);
        existingPlan.addGoal(existingGoal);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("candidate@example.test");
        when(users.findByEmailIgnoreCase("candidate@example.test")).thenReturn(Optional.of(owner));
        when(analyses.findById(8L)).thenReturn(Optional.of(analysis));
        when(plans.findByUserIdAndAnalysisId(14L, 8L)).thenReturn(List.of(existingPlan));

        ProgressPlanController controller = new ProgressPlanController(plans, users, analyses, offers, new ProgressPlanService());

        var result = controller.create(new ProgressPlanCreateRequest(8L, null, null, null, List.of("Java")), auth);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(22L, result.getBody().id());
        verify(plans, never()).save(any(ProgressPlan.class));
    }

    @Test
    void statusTransitionsPersistAndProgressPercentageIsCalculatedWithoutDivisionByZero() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ProgressPlanRepository plans = mock(ProgressPlanRepository.class);
        UserAccount owner = new UserAccount("Candidate", "candidate@example.test", "hash", Role.USER);
        setId(owner, 15L);
        ProgressPlan plan = new ProgressPlan(owner, null, null, "Plan", "Summary");
        setId(plan, 41L);
        ProgressGoal goalOne = new ProgressGoal(plan, "Java", "Objective 1", "Activity 1", "Exercise 1", 0);
        ProgressGoal goalTwo = new ProgressGoal(plan, "SQL", "Objective 2", "Activity 2", "Exercise 2", 1);
        setId(goalOne, 111L);
        setId(goalTwo, 222L);
        plan.addGoal(goalOne);
        plan.addGoal(goalTwo);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("candidate@example.test");
        when(users.findByEmailIgnoreCase("candidate@example.test")).thenReturn(Optional.of(owner));
        when(plans.findByIdAndUserEmail(41L, "candidate@example.test")).thenReturn(Optional.of(plan));
        when(plans.save(plan)).thenReturn(plan);

        ProgressPlanController controller = new ProgressPlanController(plans, users, mock(AnalysisRecordRepository.class), mock(JobOfferRepository.class), new ProgressPlanService());

        var updated = controller.updateGoalStatus(41L, 111L, new GoalStatusUpdateRequest(ProgressStatus.DONE), auth);

        assertEquals(ProgressStatus.DONE, updated.goals().getFirst().status());
        assertEquals(50.0, plan.getProgressPercentage(), 0.01);
        assertEquals(0.0, new ProgressPlan(owner, null, null, "Empty", "Summary").getProgressPercentage(), 0.01);
    }

    @Test
    void anotherUsersPlanCannotBeConsultedOrModified() {
        ProgressPlanRepository plans = mock(ProgressPlanRepository.class);
        UserRepository users = mock(UserRepository.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("candidate@example.test");
        when(users.findByEmailIgnoreCase("candidate@example.test")).thenReturn(Optional.of(new UserAccount("Candidate", "candidate@example.test", "hash", Role.USER)));
        when(plans.findByIdAndUserEmail(99L, "candidate@example.test")).thenReturn(Optional.empty());

        ProgressPlanController controller = new ProgressPlanController(plans, users, mock(AnalysisRecordRepository.class), mock(JobOfferRepository.class), new ProgressPlanService());

        ResponseStatusException consultation = assertThrows(ResponseStatusException.class, () -> controller.get(99L, auth));
        assertEquals(404, consultation.getStatusCode().value());
    }

    private static void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
