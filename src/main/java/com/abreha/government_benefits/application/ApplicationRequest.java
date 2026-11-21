package com.abreha.government_benefits.application;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ApplicationRequest extends ApplicantApplicationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

}
