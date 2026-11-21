package com.abreha.government_benefits.passwordreset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @Modifying
    @Query("""
            update PasswordResetToken token set token.usedAt = :now, token.version = token.version + 1
            where token.userId = :userId and token.usedAt is null
            """)
    void invalidateAll(Long userId, LocalDateTime now);

    @Modifying
    @Query("""
            update PasswordResetToken token set token.usedAt = :now, token.version = token.version + 1
            where token.id = :id and token.usedAt is null and token.expiresAt > :now
            """)
    int consume(Long id, LocalDateTime now);
}
