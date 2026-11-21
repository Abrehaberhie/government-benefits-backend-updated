package com.abreha.government_benefits.eligibility;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EligibilityRuleRequest {

    @NotNull(message = "Benefit ID is required")
    @Min(value = 1, message = "Benefit ID must be positive")
    private Long benefitId;

    @NotNull(message = "Household size is required")
    @Min(value = 1, message = "Household size must be at least 1")
    private Integer householdSize;

    @NotNull(message = "Maximum annual income is required")
    @DecimalMin(
            value = "0.00",
            message = "Maximum annual income cannot be negative"
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Income must have at most 13 whole digits and 2 decimal places"
    )
    private BigDecimal maximumAnnualIncome;

    @NotNull(message = "Minimum age is required")
    @Min(value = 0, message = "Minimum age cannot be negative")
    private Integer minimumAge;
}
