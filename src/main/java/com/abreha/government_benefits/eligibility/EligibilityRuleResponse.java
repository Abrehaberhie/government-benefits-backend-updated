package com.abreha.government_benefits.eligibility;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class EligibilityRuleResponse {

    private final Long id;
    private final Long benefitId;
    private final String benefitName;
    private final Integer householdSize;
    private final BigDecimal maximumAnnualIncome;
    private final Integer minimumAge;

    public static EligibilityRuleResponse from(EligibilityRule rule) {
        return new EligibilityRuleResponse(
                rule.getId(),
                rule.getBenefit().getId(),
                rule.getBenefit().getName(),
                rule.getHouseholdSize(),
                rule.getMaximumAnnualIncome(),
                rule.getMinimumAge()
        );
    }
}
