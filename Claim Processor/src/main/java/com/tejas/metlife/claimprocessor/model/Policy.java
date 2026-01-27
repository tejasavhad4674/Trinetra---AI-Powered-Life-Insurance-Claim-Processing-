package com.tejas.metlife.claimprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Policy entity representing policy metadata. Persisted to Azure SQL via JPA.
 */
@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String policyNumber;

    private String policyHolderName;

    private LocalDate dateOfBirth;

    private LocalDate issueDate;

    private LocalDate maturityDate;

    private int suicideCoverageAfterYears = 1;

    private boolean coversAccident = true;

    private boolean coversNaturalDeath = true;

    private boolean coversDisease = true;

    private String status = "ACTIVE";

}
