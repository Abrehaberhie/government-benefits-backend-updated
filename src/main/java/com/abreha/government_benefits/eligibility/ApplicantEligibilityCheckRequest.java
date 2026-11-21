package com.abreha.government_benefits.eligibility;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ApplicantEligibilityCheckRequest {

    @NotNull(message = "Benefit ID is required")
    @Min(value = 1, message = "Benefit ID must be positive")
    private Long benefitId;

    @NotNull(message = "Household size is required")
    @Min(value = 1, message = "Household size must be at least 1")
    private Integer householdSize;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth cannot be in the future")
    private LocalDate dateOfBirth;

    @NotNull(message = "Annual household income is required")
    @DecimalMin(value = "0.00", message = "Annual household income cannot be negative")
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Income must have at most 13 whole digits and 2 decimal places"
    )
    private BigDecimal annualHouseholdIncome;
}