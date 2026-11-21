package com.abreha.government_benefits.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** School-project intake questions, not official government eligibility rules. */
@Component
public class ProgramQuestions {
    public record Question(String key, String label, String type,
                           List<String> options, BigDecimal min,
                           BigDecimal max, String step) {}
    public record Definition(String programName, String guidance,
                             List<Question> questions) {}

    private static Question money(String key, String label) {
        return new Question(key, label, "number", List.of(),
                BigDecimal.ZERO, new BigDecimal("1000000000"), "0.01");
    }

    private static Question count(String key, String label, int min, int max) {
        return new Question(key, label, "number", List.of(),
                BigDecimal.valueOf(min), BigDecimal.valueOf(max), "1");
    }

    private static Question choice(String key, String label, String... options) {
        return new Question(key, label, "select", List.of(options), null, null, null);
    }

    private static Question text(String key, String label) {
        return new Question(key, label, "text", List.of(), null, null, null);
    }

    private static final List<Question> COMMON = List.of(
            count("householdSize", "Household size", 1, 30),
            money("annualHouseholdIncome", "Annual household income ($)")
    );

    public Definition forProgram(String name) {
        String normalized = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        String guidance;
        List<Question> specific;
        switch (normalized) {
            case "housing assistance" -> {
                guidance = "This form collects your housing situation and monthly rent.";
                specific = List.of(
                        choice("housingSituation", "Current housing situation", "Renting", "Own home", "Temporary housing", "No stable housing"),
                        money("monthlyRent", "Monthly rent ($; enter 0 if none)"));
            }
            case "food assistance" -> {
                guidance = "This form collects your food expenses and current food support.";
                specific = List.of(
                        money("monthlyFoodCosts", "Monthly food expenses ($)"),
                        choice("receivesFoodSupport", "Do you currently receive food assistance?", "Yes", "No"));
            }
            case "healthcare assistance" -> {
                guidance = "This form collects your insurance coverage and out-of-pocket medical expenses.";
                specific = List.of(
                        choice("insuranceStatus", "Health insurance coverage", "Insured", "Uninsured", "Coverage ending"),
                        money("monthlyMedicalCosts", "Monthly out-of-pocket medical costs ($)"));
            }
            case "unemployment assistance" -> {
                guidance = "This form collects your employment status and details about when work ended.";
                specific = List.of(
                        choice("employmentStatus", "Current employment status", "Unemployed", "Employed part time", "Employed full time", "Self-employed"),
                        new Question("lastEmploymentDate", "Last day of employment", "date", List.of(), null, null, null),
                        choice("jobSeparationReason", "Reason employment ended", "Laid off", "Contract ended", "Dismissed", "Resigned", "Other"));
            }
            case "education assistance" -> {
                guidance = "This form collects your enrollment status and education expenses.";
                specific = List.of(
                        choice("enrollmentStatus", "Enrollment status", "Enrolled full time", "Enrolled part time", "Accepted for upcoming term", "Not currently enrolled"),
                        text("schoolName", "School or college name (enter N/A if none)"),
                        money("annualEducationCosts", "Annual tuition, books, and fees ($)"));
            }
            case "childcare assistance" -> {
                guidance = "This form collects information about children needing care and childcare expenses.";
                specific = List.of(
                        count("childrenNeedingCare", "Number of children needing care", 0, 30),
                        count("youngestChildAge", "Youngest child's age (years; enter 0 if none)", 0, 17),
                        money("monthlyChildcareCosts", "Monthly childcare costs ($)"),
                        choice("careReason", "Reason childcare is needed", "Working", "Attending school or training", "Seeking work", "Other"));
            }
            case "disability assistance" -> {
                guidance = "This form collects information about disability status and support needs. Do not enter a medical diagnosis.";
                specific = List.of(
                        choice("disabilityStatus", "Disability status", "Documented disability", "Assessment pending", "Not yet assessed"),
                        choice("supportNeed", "Primary support need", "Daily living expenses", "Mobility or assistive equipment", "Personal assistance", "Other"),
                        money("monthlySupportCosts", "Monthly disability-related support costs ($)"));
            }
            case "utility assistance" -> {
                guidance = "This form collects your utility expenses and details about overdue bills.";
                specific = List.of(
                        choice("utilityType", "Utility needing assistance", "Electricity", "Heating or gas", "Water", "Multiple utilities"),
                        money("monthlyUtilityCosts", "Monthly utility costs ($)"),
                        money("overdueUtilityAmount", "Overdue utility amount ($; enter 0 if none)"));
            }
            default -> throw badRequest("No application questions are configured for this benefit. Contact an administrator.");
        }
        List<Question> all = new ArrayList<>(COMMON);
        all.addAll(specific);
        return new Definition(name, guidance, List.copyOf(all));
    }

    /** Validates the selected program only and stores readable labels with the answers. */
    public Map<String, String> validate(String programName, Map<String, String> answers) {
        Definition definition = forProgram(programName);
        if (answers == null) {
            throw badRequest("Application answers are required.");
        }
        Set<String> expected = new HashSet<>();
        definition.questions().forEach(q -> expected.add(q.key()));
        if (!expected.containsAll(answers.keySet())) {
            throw badRequest("Remove answers that do not belong to the selected benefit.");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Question q : definition.questions()) {
            String value = answers.get(q.key());
            if (value == null || value.isBlank()) {
                throw badRequest(q.label() + " is required.");
            }
            value = value.trim();
            if (value.length() > 200) {
                throw badRequest(q.label() + " must be 200 characters or fewer.");
            }
            switch (q.type()) {
                case "number" -> {
                    try {
                        if (!value.matches("[0-9]{1,10}(\\.[0-9]{1,2})?")) {
                            throw badRequest(q.label() + " must use a plain number with at most two decimal places.");
                        }
                        BigDecimal number = new BigDecimal(value);
                        if (number.compareTo(q.min()) < 0 || number.compareTo(q.max()) > 0
                                || number.remainder(new BigDecimal(q.step())).signum() != 0) {
                            throw badRequest(q.label() + " is outside the allowed range or precision.");
                        }
                        value = number.toPlainString();
                    } catch (NumberFormatException ex) {
                        throw badRequest(q.label() + " must be a valid number.");
                    }
                }
                case "select" -> {
                    if (!q.options().contains(value)) {
                        throw badRequest("Choose a valid option for " + q.label() + ".");
                    }
                }
                case "date" -> {
                    try {
                        LocalDate date = LocalDate.parse(value);
                        if (date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.of(1900, 1, 1))) {
                            throw badRequest(q.label() + " must be between 1900-01-01 and today.");
                        }
                        value = date.toString();
                    } catch (DateTimeParseException ex) {
                        throw badRequest(q.label() + " must be a valid date.");
                    }
                }
                default -> { /* Text already has required and length checks. */ }
            }
            result.put(q.label(), value);
        }
        return result;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
