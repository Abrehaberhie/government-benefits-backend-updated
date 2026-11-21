package com.abreha.government_benefits.application;

import com.abreha.government_benefits.benefit.Benefit;
import com.abreha.government_benefits.benefit.BenefitRepository;
import com.abreha.government_benefits.eligibility.EligibilityCheckResponse;
import com.abreha.government_benefits.eligibility.EligibilityService;
import com.abreha.government_benefits.notification.NotificationService;
import com.abreha.government_benefits.user.User;
import com.abreha.government_benefits.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final BenefitRepository benefitRepository;
    private final NotificationService notificationService;
    private final ProgramQuestions programQuestions;
    private final EligibilityService eligibilityService;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            UserRepository userRepository,
            BenefitRepository benefitRepository,
            NotificationService notificationService,
            ProgramQuestions programQuestions,
            EligibilityService eligibilityService
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.benefitRepository = benefitRepository;
        this.notificationService = notificationService;
        this.programQuestions = programQuestions;
        this.eligibilityService = eligibilityService;
    }

    @Transactional
    public ApplicationResponse submit(Long userId, ApplicantApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        return submit(user, request);
    }

    private ApplicationResponse submit(User user, ApplicantApplicationRequest request) {
        Benefit benefit = benefitRepository.findById(request.getBenefitId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Benefit not found"));

        if (!benefit.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Benefit is not active");
        }

        Map<String, String> answers = request.getAnswers();
        Map<String, String> savedAnswers = programQuestions.validate(benefit.getName(), answers);
        int householdSize = new BigDecimal(answers.get("householdSize").trim()).intValueExact();
        BigDecimal income = new BigDecimal(answers.get("annualHouseholdIncome").trim());

        EligibilityCheckResponse eligibility = eligibilityService.evaluate(
                user, benefit, householdSize, income, request.getDateOfBirth());

        String reason = String.join(". ", eligibility.getReasons()) + ".";
        String status = switch (eligibility.getResult()) {
            case "ELIGIBLE" -> "APPROVED";
            case "NOT_ELIGIBLE" -> "DENIED";
            case "UNDETERMINED" -> throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "An eligibility decision could not be made. " + reason);
            default -> throw new IllegalStateException(
                    "Unexpected eligibility result: " + eligibility.getResult());
        };

        Application application = new Application();
        application.setUser(user);
        application.setBenefit(benefit);
        application.setAnswers(savedAnswers);
        application.setStatus(status);
        application.setDecisionReason(reason);

        Application saved = applicationRepository.save(application);
        notifyDecision(saved);
        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAll() {
        return applicationRepository.findAll().stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found"));

        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse updateStatus(Long id, String status) {
        if (!"APPROVED".equals(status) && !"DENIED".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status must be APPROVED or DENIED");
        }

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found"));

        // Repeating the same status does not create another notification.
        if (status.equals(application.getStatus())) {
            return ApplicationResponse.from(application);
        }

        if (application.getDecisionReason() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This application's status was decided automatically.");
        }

        application.setStatus(status);
        Application saved = applicationRepository.save(application);
        notifyDecision(saved);
        return ApplicationResponse.from(saved);
    }

    private void notifyDecision(Application saved) {
        notificationService.createNotification(
                saved.getUser().getId(),
                saved.getId(),
                "Application Status Updated",
                "Your application #" + saved.getId()
                        + " has been "
                        + saved.getStatus().toLowerCase(Locale.ROOT)
                        + "."
        );
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(String email) {
        return applicationRepository
                .findByUser_EmailOrderBySubmittedAtDescIdDesc(email)
                .stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getMyApplicationById(Long id, String email) {
        Application application = applicationRepository
                .findByIdAndUser_Email(id, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found"));

        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse submitMyApplication(
            String email,
            ApplicantApplicationRequest request
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        return submit(user, request);
    }
}