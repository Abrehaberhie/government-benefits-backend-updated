package com.abreha.government_benefits.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicantApplicationRequest {

    @NotNull(message = "Benefit ID is required")
    @Min(value = 1, message = "Benefit ID must be positive")
    private Long benefitId;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth cannot be in the future")
    private LocalDate dateOfBirth;

    @NotNull(message = "Application answers are required")
    @Size(max = 12, message = "Too many application answers")
    private Map<String, String> answers;
}