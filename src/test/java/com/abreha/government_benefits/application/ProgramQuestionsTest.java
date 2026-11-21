package com.abreha.government_benefits.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProgramQuestionsTest {
    private final ProgramQuestions catalog = new ProgramQuestions();

    private Map<String, String> validAnswers(String program) {
        Map<String, String> answers = new LinkedHashMap<>();
        for (ProgramQuestions.Question q : catalog.forProgram(program).questions()) {
            answers.put(q.key(), switch (q.type()) {
                case "number" -> q.min().toPlainString();
                case "select" -> q.options().getFirst();
                case "date" -> "2026-01-01";
                default -> "UDC";
            });
        }
        return answers;
    }

    @ParameterizedTest
    @ValueSource(strings = {"Housing Assistance", "Food Assistance", "Healthcare Assistance",
            "Unemployment Assistance", "Education Assistance", "Childcare Assistance",
            "Disability Assistance", "Utility Assistance"})
    void acceptsCompleteAnswersAndReturnsReadableLabels(String program) {
        Map<String, String> answers = validAnswers(program);
        Map<String, String> saved = catalog.validate(program, answers);
        assertEquals(answers.size(), saved.size());
        assertTrue(saved.containsKey("Household size"));
        assertTrue(saved.containsKey("Annual household income ($)"));
    }

    @Test void rejectsMissingAndWrongProgramAnswers() {
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Housing Assistance", null));
        Map<String, String> answers = validAnswers("Housing Assistance");
        answers.remove("monthlyRent");
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Housing Assistance", answers));
        answers.put("monthlyRent", "0");
        answers.put("schoolName", "UDC");
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Housing Assistance", answers));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.001", "NaN", "1e9", "0E-999999999", "1000000001", ""})
    void rejectsInvalidMoney(String value) {
        Map<String, String> answers = validAnswers("Housing Assistance");
        answers.put("monthlyRent", value);
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Housing Assistance", answers));
    }

    @Test void rejectsFractionalHouseholdSizeAndInvalidChoice() {
        Map<String, String> answers = validAnswers("Housing Assistance");
        answers.put("householdSize", "1.5");
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Housing Assistance", answers));
        answers.put("householdSize", "2");
        answers.put("housingSituation", "Invented option");
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Housing Assistance", answers));
    }

    @Test void rejectsFutureAndMalformedDates() {
        Map<String, String> answers = validAnswers("Unemployment Assistance");
        answers.put("lastEmploymentDate", LocalDate.now().plusDays(1).toString());
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Unemployment Assistance", answers));
        answers.put("lastEmploymentDate", "not-a-date");
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Unemployment Assistance", answers));
    }

    @Test void rejectsBlankAndOversizedText() {
        Map<String, String> answers = validAnswers("Education Assistance");
        answers.put("schoolName", "  ");
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Education Assistance", answers));
        answers.put("schoolName", "x".repeat(201));
        assertThrows(ResponseStatusException.class, () -> catalog.validate("Education Assistance", answers));
    }

    @Test void matchesNamesWithoutCaseOrSurroundingWhitespaceAndRejectsUnknownPrograms() {
        assertEquals(4, catalog.forProgram("  HOUSING ASSISTANCE  ").questions().size());
        assertThrows(ResponseStatusException.class, () -> catalog.forProgram("Other program"));
    }
}
