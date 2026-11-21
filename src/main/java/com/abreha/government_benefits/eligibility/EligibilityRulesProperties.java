package com.abreha.government_benefits.eligibility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "eligibility")
public record EligibilityRulesProperties(
        @NotEmpty Map<String, @Valid Program> programs
) {
    Program forProgram(String benefitName) {
        String key = benefitName.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-");
        return programs.get(key);
    }

    public record Program(
            @NotNull @Min(0) Integer minimumAge,
            @NotEmpty Map<
                    @Min(1) @Max(30) Integer,
                    @NotNull @DecimalMin("0.00")
                    @Digits(integer = 13, fraction = 2) BigDecimal
                    > annualIncomeLimitByHouseholdSize
    ) {}
}
