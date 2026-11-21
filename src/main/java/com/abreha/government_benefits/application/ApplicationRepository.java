package com.abreha.government_benefits.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    List<Application> findByUser_EmailOrderBySubmittedAtDescIdDesc(
            String email
    );

    Optional<Application> findByIdAndUser_Email(
            Long id,
            String email
    );
}