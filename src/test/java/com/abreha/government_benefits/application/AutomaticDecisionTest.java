package com.abreha.government_benefits.application;

import com.abreha.government_benefits.benefit.Benefit;
import com.abreha.government_benefits.benefit.BenefitRepository;
import com.abreha.government_benefits.eligibility.EligibilityRule;
import com.abreha.government_benefits.eligibility.EligibilityRuleRepository;
import com.abreha.government_benefits.eligibility.EligibilityService;
import com.abreha.government_benefits.notification.NotificationService;
import com.abreha.government_benefits.user.User;
import com.abreha.government_benefits.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AutomaticDecisionTest {
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final BenefitRepository benefits = mock(BenefitRepository.class);
    private final EligibilityRuleRepository rules = mock(EligibilityRuleRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final ApplicationService service = new ApplicationService(
            applications, users, benefits, notifications, new ProgramQuestions(),
            new EligibilityService(rules, benefits, users));
    private final User applicant = new User();
    private final Benefit housing = new Benefit();

    @BeforeEach
    void configureProgram() {
        applicant.setId(7L);
        applicant.setDateOfBirth(LocalDate.now().minusYears(30));
        when(users.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicant));
        when(users.findById(7L)).thenReturn(Optional.of(applicant));

        housing.setId(1L);
        housing.setName("Housing Assistance");
        housing.setActive(true);
        when(benefits.findById(1L)).thenReturn(Optional.of(housing));

        EligibilityRule rule = new EligibilityRule();
        rule.setMinimumAge(18);
        rule.setMaximumAnnualIncome(new BigDecimal("30000.00"));
        when(rules.findByBenefit_IdAndHouseholdSize(1L, 2)).thenReturn(Optional.of(rule));

        when(applications.save(any(Application.class))).thenAnswer(invocation -> {
            Application saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });
    }

    @Test
    void applicantAtIncomeLimitIsApprovedAndNotified() {
        ApplicationResponse decision = service.submitMyApplication(
                "applicant@example.com", request("2.00", "30000.00"));

        assertEquals("APPROVED", decision.getStatus());
        assertEquals("Applicant meets the configured income and age rules.", decision.getDecisionReason());
        assertEquals("Renting", decision.getAnswers().get("Current housing situation"));
        verify(applications).save(argThat(value -> "APPROVED".equals(value.getStatus())));
        verify(notifications).createNotification(eq(7L), eq(12L), anyString(), contains("approved"));
    }

    @Test
    void applicantOverIncomeLimitIsDeniedOnTheAdminSubmissionEndpointToo() {
        ApplicationResponse decision = service.submit(7L, request("2", "30000.01"));

        assertEquals("DENIED", decision.getStatus());
        assertTrue(decision.getDecisionReason().contains("income exceeds the limit of 30000.00"));
        verify(applications).save(argThat(value -> "DENIED".equals(value.getStatus())));
        verify(notifications).createNotification(eq(7L), eq(12L), anyString(), contains("denied"));
    }

    @Test
    void underageApplicantIsDenied() {
        applicant.setDateOfBirth(LocalDate.now().minusYears(17));

        ApplicationResponse decision = service.submit(7L, request("2", "20000"));
        assertEquals("DENIED", decision.getStatus());
        assertTrue(decision.getDecisionReason().contains("at least 18 years old"));
    }

    @Test
    void missingRuleReturnsAnExplanationWithoutSaving() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.submit(7L, request("3", "20000")));

        assertEquals(422, error.getStatusCode().value());
        assertTrue(error.getReason().contains("No rule exists for this benefit and household size"));
        verifyNoInteractions(applications, notifications);
    }

    @Test
    void missingBirthDateReturnsAnExplanationWithoutSaving() {
        applicant.setDateOfBirth(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.submit(7L, request("2", "20000")));

        assertEquals(422, error.getStatusCode().value());
        assertTrue(error.getReason().contains("Date of birth is required"));
        verifyNoInteractions(applications, notifications);
    }

    @Test
    void staffCannotReverseAnAutomaticDecision() {
        Application application = new Application();
        application.setStatus("APPROVED");
        application.setDecisionReason("Applicant meets the configured income and age rules.");
        when(applications.findById(12L)).thenReturn(Optional.of(application));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(12L, "DENIED"));

        assertEquals(409, error.getStatusCode().value());
        assertEquals("APPROVED", application.getStatus());
        verify(applications, never()).save(any(Application.class));
        verifyNoInteractions(notifications);
    }

    private ApplicantApplicationRequest request(String householdSize, String income) {
        ApplicantApplicationRequest request = new ApplicantApplicationRequest();
        request.setBenefitId(1L);
        request.setAnswers(Map.of(
                "householdSize", householdSize,
                "annualHouseholdIncome", income,
                "housingSituation", "Renting",
                "monthlyRent", "1200"));
        return request;
    }
}
