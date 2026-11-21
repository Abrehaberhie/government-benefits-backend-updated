package com.abreha.government_benefits.benefit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/benefits")

public class BenefitController {

    private final BenefitService benefitService;

    public BenefitController(BenefitService benefitService) {
        this.benefitService = benefitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Benefit createBenefit(@RequestBody Benefit benefit) {
        return benefitService.createBenefit(benefit);
    }

    @GetMapping
    public List<Benefit> getAllBenefits() {
        return benefitService.getAllBenefits();
    }

    @GetMapping("/{id}")
    public Benefit getBenefitById(@PathVariable Long id) {
        return benefitService.getBenefitById(id);
    }
}
