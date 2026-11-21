/*package com.abreha.government_benefits.eligibility;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/eligibility")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(
            EligibilityService eligibilityService
    ) {
        this.eligibilityService = eligibilityService;
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public EligibilityRuleResponse createRule(
            @Valid @RequestBody EligibilityRuleRequest request
    ) {
        return eligibilityService.createRule(request);
    }

    @GetMapping("/rules")
    public List<EligibilityRuleResponse> getAllRules() {
        return eligibilityService.getAllRules();
    }

    @PostMapping("/check")
    public EligibilityCheckResponse check(
            @Valid @RequestBody EligibilityCheckRequest request
    ) {
        return eligibilityService.check(request);
    }

    @PostMapping("/mine/check")
    public EligibilityCheckResponse checkMyEligibility(
            @Valid @RequestBody ApplicantEligibilityCheckRequest request,
            Principal principal
    ) {
        return eligibilityService.checkMyEligibility(
                principal.getName(),
                request
        );
    }
}

 */
package com.abreha.government_benefits.eligibility;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/eligibility")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @GetMapping("/rules")
    public List<EligibilityRuleResponse> getAllRules() {
        return eligibilityService.getAllRules();
    }

    @PostMapping("/check")
    public EligibilityCheckResponse check(
            @Valid @RequestBody EligibilityCheckRequest request
    ) {
        return eligibilityService.check(request);
    }

    @PostMapping("/mine/check")
    public EligibilityCheckResponse checkMyEligibility(
            @Valid @RequestBody ApplicantEligibilityCheckRequest request,
            Principal principal
    ) {
        return eligibilityService.checkMyEligibility(
                principal.getName(),
                request
        );
    }
}
