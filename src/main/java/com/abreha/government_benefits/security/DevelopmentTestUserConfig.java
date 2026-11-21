package com.abreha.government_benefits.security;

import com.abreha.government_benefits.user.Role;
import com.abreha.government_benefits.user.User;
import com.abreha.government_benefits.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Configuration
@Profile("local")
public class DevelopmentTestUserConfig {

    @Bean
    public ApplicationRunner createTestUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${TEST_APPLICANT_PASSWORD:}") String applicantPassword,
            @Value("${TEST_CASE_WORKER_PASSWORD:}") String caseWorkerPassword,
            @Value("${TEST_ADMIN_PASSWORD:}") String adminPassword
    ) {
        return args -> {
            createIfMissing(
                    userRepository,
                    passwordEncoder,
                    "security.test@example.com",
                    "Applicant",
                    Role.APPLICANT,
                    applicantPassword,
                    "TEST_APPLICANT_PASSWORD"
            );

            createIfMissing(
                    userRepository,
                    passwordEncoder,
                    "caseworker.test@example.com",
                    "CaseWorker",
                    Role.CASE_WORKER,
                    caseWorkerPassword,
                    "TEST_CASE_WORKER_PASSWORD"
            );

            createIfMissing(
                    userRepository,
                    passwordEncoder,
                    "admin.test@example.com",
                    "Admin",
                    Role.ADMIN,
                    adminPassword,
                    "TEST_ADMIN_PASSWORD"
            );
        };
    }

    private void createIfMissing(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String firstName,
            Role role,
            String password,
            String environmentVariable
    ) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        if (password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Set " + environmentVariable
                            + " to at least 12 characters "
                            + "and at most 72 UTF-8 bytes."
            );
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName("Test");
        user.setEmail(email);
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);
    }
}
