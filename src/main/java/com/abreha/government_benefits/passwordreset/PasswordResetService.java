package com.abreha.government_benefits.passwordreset;

import com.abreha.government_benefits.user.User;
import com.abreha.government_benefits.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);
    private final SecureRandom random = new SecureRandom();
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final SessionRegistry sessions;
    private final ObjectProvider<JavaMailSender> mail;
    private final String from;
    private final String frontendUrl;
    private final String mailHost;

    PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                         PasswordEncoder encoder, SessionRegistry sessions,
                         ObjectProvider<JavaMailSender> mail,
                         @Value("${app.password-reset.from}") String from,
                         @Value("${app.password-reset.frontend-url}") String frontendUrl,
                         @Value("${spring.mail.host}") String mailHost) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.sessions = sessions;
        this.mail = mail;
        this.from = from;
        this.frontendUrl = frontendUrl;
        this.mailHost = mailHost;
    }

    boolean canSendMail() {
        return !mailHost.isBlank() && !from.isBlank() && mail.getIfAvailable() != null;
    }

    @Async("passwordResetExecutor")
    @Transactional
    public void requestReset(String email) {
        User user = users.findLockedByEmail(email.trim()).orElse(null);
        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (tokens.findFirstByUserIdOrderByCreatedAtDescIdDesc(user.getId())
                .filter(token -> token.getCreatedAt().isAfter(now.minusMinutes(1))).isPresent()) {
            return;
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.invalidateAll(user.getId(), now);
        tokens.save(new PasswordResetToken(user.getId(), hash(token), now));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Reset your Government Benefits Portal password");
        message.setText("""
                Open this link within 30 minutes to reset your password:

                %s/reset-password#token=%s

                If you did not request a reset, you can ignore this email.
                """.formatted(frontendUrl.replaceAll("/+$", ""), token));
        try {
            mail.getObject().send(message);
        } catch (MailException error) {
            LOG.warn("Password reset email delivery failed", error);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String password) {
        PasswordResetToken token = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(PasswordResetService::invalidToken);
        User user = users.findLockedById(token.getUserId())
                .orElseThrow(PasswordResetService::invalidToken);
        LocalDateTime now = LocalDateTime.now();
        if (tokens.consume(token.getId(), now) != 1) {
            throw invalidToken();
        }

        user.setPassword(encoder.encode(password));
        tokens.invalidateAll(user.getId(), now);
        String email = user.getEmail();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Object principal : sessions.getAllPrincipals()) {
                    if (principal instanceof UserDetails details && email.equalsIgnoreCase(details.getUsername())) {
                        sessions.getAllSessions(principal, false).forEach(SessionInformation::expireNow);
                    }
                }
            }
        });
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "This reset link is invalid or expired. Request a new link.");
    }
}