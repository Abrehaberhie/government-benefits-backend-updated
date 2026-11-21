package com.abreha.government_benefits.eligibility;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EligibilityRuleRepository
        extends JpaRepository<EligibilityRule, Long> {

    Optional<EligibilityRule> findByBenefit_IdAndHouseholdSize(
            Long benefitId,
            Integer householdSize
    );
}