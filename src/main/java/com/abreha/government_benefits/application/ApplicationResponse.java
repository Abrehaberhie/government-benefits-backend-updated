package com.abreha.government_benefits.application;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

@Getter
@AllArgsConstructor

public class ApplicationResponse {

    private final Long id;
    private final Long userId;
    private final Long benefitId;
    private final String benefitName;
    private final String status;
    private final String decisionReason;
    private final LocalDateTime submittedAt;
    private final Map<String, String> answers;

    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getUser().getId(),
                application.getBenefit().getId(),
                application.getBenefit().getName(),
                application.getStatus(),
                application.getDecisionReason(),
                application.getSubmittedAt(),
                Collections.unmodifiableMap(new LinkedHashMap<>(application.getAnswers()))
        );
    }
}
