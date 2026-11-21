package com.abreha.government_benefits.application;

import com.abreha.government_benefits.benefit.Benefit;
import com.abreha.government_benefits.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor

public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @Column(nullable = false)
    private String status = "SUBMITTED";

    @Column(name = "decision_reason", length = 1000, updatable = false)
    private String decisionReason;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @ElementCollection
    @CollectionTable(name = "application_answers",
            joinColumns = @JoinColumn(name = "application_id"))
    @MapKeyColumn(name = "question_label", length = 120)
    @Column(name = "answer_value", length = 200, nullable = false)
    private Map<String, String> answers = new LinkedHashMap<>();

    @PrePersist
    void setSubmissionTime() {
        submittedAt = LocalDateTime.now();
    }
}
