package com.abreha.government_benefits.eligibility;

import com.abreha.government_benefits.benefit.Benefit;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "eligibility_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_eligibility_benefit_household",
                columnNames = {"benefit_id", "household_size"}
        )
)
@Getter
@Setter
@NoArgsConstructor

public class EligibilityRule {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @NotNull
    @Min(1)
    @Column(name = "household_size", nullable = false)
    private Integer householdSize;

    @NotNull
    @DecimalMin("0.00")
    @Column(
            name = "maximum_annual_income",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal maximumAnnualIncome;

    @NotNull
    @Min(0)
    @Column(name = "minimum_age", nullable = false)
    private Integer minimumAge = 0;
}

