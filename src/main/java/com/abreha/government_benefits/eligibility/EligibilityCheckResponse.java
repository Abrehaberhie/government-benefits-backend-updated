package com.abreha.government_benefits.eligibility;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class EligibilityCheckResponse {

    private final Long userId;
    private final Long benefitId;
    private final String benefitName;
    private final String result;
    private final List<String> reasons;
    private final LocalDateTime checkedAt;
}
