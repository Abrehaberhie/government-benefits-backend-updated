package com.abreha.government_benefits.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ApplicationStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;
}
