package com.abreha.government_benefits.eligibility;

import com.abreha.government_benefits.benefit.Benefit;
import com.abreha.government_benefits.benefit.BenefitRepository;
import com.abreha.government_benefits.user.User;
import com.abreha.government_benefits.user.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Service
@EnableConfigurationProperties(EligibilityRulesProperties.class)
public class EligibilityService {

    private final EligibilityRulesProperties rules;
    private final BenefitRepository benefitRepository;
    private final UserRepository userRepository;

    public EligibilityService(
            EligibilityRulesProperties rules,
            BenefitRepository benefitRepository,
            UserRepository userRepository
    ) {
        this.rules = rules;
        this.benefitRepository = benefitRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EligibilityRuleResponse> getAllRules() {
        return benefitRepository.findAll().stream()
                .flatMap(benefit -> {
                    EligibilityRulesProperties.Program program =
                            rules.forProgram(benefit.getName());

                    if (program == null) {
                        return Stream.empty();
                    }

                    return program.annualIncomeLimitByHouseholdSize().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(limit -> new EligibilityRuleResponse(
                                    null,
                                    benefit.getId(),
                                    benefit.getName(),
                                    limit.getKey(),
                                    limit.getValue(),
                                    program.minimumAge()
                            ));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public EligibilityCheckResponse check(EligibilityCheckRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Benefit benefit = findBenefit(request.getBenefitId());
        EligibilityCheckResponse result = evaluate(
                user,
                benefit,
                request.getHouseholdSize(),
                request.getAnnualHouseholdIncome(),
                request.getDateOfBirth()
        );

        if ("ELIGIBLE".equals(result.getResult())) {
            List<String> reasons = new ArrayList<>(result.getReasons());
            reasons.add("This preliminary check does not approve an application");
            return response(user, benefit, result.getResult(), reasons);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public EligibilityCheckResponse evaluate(
            User user,
            Benefit benefit,
            int householdSize,
            BigDecimal annualHouseholdIncome,
            LocalDate dateOfBirth
    ) {
        LocalDate today = LocalDate.now();
        if (dateOfBirth == null || dateOfBirth.isAfter(today)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid date of birth to check the age requirement");
        }

        if (!benefit.isActive()) {
            return response(user, benefit, "NOT_ELIGIBLE",
                    List.of("This benefit is not currently active"));
        }

        EligibilityRulesProperties.Program program = rules.forProgram(benefit.getName());
        BigDecimal incomeLimit = program == null
                ? null
                : program.annualIncomeLimitByHouseholdSize().get(householdSize);

        if (incomeLimit == null) {
            return response(user, benefit, "UNDETERMINED", List.of(
                    "No eligibility rule is configured for %s for a household of %d"
                            .formatted(benefit.getName(), householdSize),
                    "Contact portal support to have the program rule configured"
            ));
        }

        List<String> passed = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        boolean incomeMeetsLimit = annualHouseholdIncome.compareTo(incomeLimit) <= 0;
        String incomeReason =
                "Your reported annual household income of %s is %s the %s limit of %s for a household of %d"
                        .formatted(
                                currency.format(annualHouseholdIncome),
                                incomeMeetsLimit ? "within" : "above",
                                benefit.getName(),
                                currency.format(incomeLimit),
                                householdSize
                        );
        (incomeMeetsLimit ? passed : failures).add(incomeReason);

        int minimumAge = program.minimumAge();
        if (minimumAge > 0) {
            boolean meetsMinimumAge = Period.between(dateOfBirth, today).getYears() >= minimumAge;
            if (meetsMinimumAge) {
                passed.add("You meet the program's minimum age of " + minimumAge);
            } else {
                failures.add("You are below the program's minimum age of " + minimumAge);
            }
        }

        if (!failures.isEmpty()) {
            return response(user, benefit, "NOT_ELIGIBLE", failures);
        }

        return response(user, benefit, "ELIGIBLE", passed);
    }

    private Benefit findBenefit(Long benefitId) {
        return benefitRepository.findById(benefitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Benefit not found"));
    }

    private EligibilityCheckResponse response(
            User user,
            Benefit benefit,
            String result,
            List<String> reasons
    ) {
        return new EligibilityCheckResponse(
                user.getId(),
                benefit.getId(),
                benefit.getName(),
                result,
                List.copyOf(reasons),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public EligibilityCheckResponse checkMyEligibility(
            String email,
            ApplicantEligibilityCheckRequest applicantRequest
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        EligibilityCheckRequest request = new EligibilityCheckRequest();
        request.setUserId(user.getId());
        request.setBenefitId(applicantRequest.getBenefitId());
        request.setHouseholdSize(applicantRequest.getHouseholdSize());
        request.setAnnualHouseholdIncome(applicantRequest.getAnnualHouseholdIncome());
        request.setDateOfBirth(applicantRequest.getDateOfBirth());

        return check(request);
    }
}