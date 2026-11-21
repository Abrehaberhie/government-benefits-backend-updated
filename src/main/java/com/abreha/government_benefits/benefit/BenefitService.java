/*package com.abreha.government_benefits.benefit;

import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class BenefitService {

    private final BenefitRepository benefitRepository;

    public BenefitService(BenefitRepository benefitRepository) {
        this.benefitRepository = benefitRepository;
    }

    public Benefit createBenefit(Benefit benefit) {
        return benefitRepository.save(benefit);
    }

    public List<Benefit> getAllBenefits() {
        return benefitRepository.findAll();
    }

    public Benefit getBenefitById(Long id) {
        return benefitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Benefit not found with ID: " + id));
    }
}


 */

package com.abreha.government_benefits.benefit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BenefitService {

    private final BenefitRepository benefitRepository;

    public BenefitService(BenefitRepository benefitRepository) {
        this.benefitRepository = benefitRepository;
    }

    public Benefit createBenefit(Benefit benefit) {
        return benefitRepository.save(benefit);
    }

    public List<Benefit> getAllBenefits() {
        return benefitRepository.findAll();
    }

    public Benefit getBenefitById(Long id) {
        return benefitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benefit not found with ID: " + id
                ));
    }
}