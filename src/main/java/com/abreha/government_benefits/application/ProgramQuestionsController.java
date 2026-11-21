package com.abreha.government_benefits.application;

import com.abreha.government_benefits.benefit.Benefit;
import com.abreha.government_benefits.benefit.BenefitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/benefits")
public class ProgramQuestionsController {
    private final BenefitRepository benefits;
    private final ProgramQuestions questions;

    public ProgramQuestionsController(BenefitRepository benefits, ProgramQuestions questions) {
        this.benefits = benefits;
        this.questions = questions;
    }

    @GetMapping("/application-questions")
    public ProgramQuestions.Definition getQuestions(@RequestParam Long benefitId) {
        Benefit benefit = benefits.findById(benefitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit not found"));
        if (!benefit.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benefit is not active");
        }
        return questions.forProgram(benefit.getName());
    }
}
