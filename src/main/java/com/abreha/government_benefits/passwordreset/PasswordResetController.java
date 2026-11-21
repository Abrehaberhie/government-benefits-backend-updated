package com.abreha.government_benefits.passwordreset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
class PasswordResetController {
    private final PasswordResetService service;

    PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        if (!service.canSendMail()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
        service.requestReset(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(request.token(), request.password());
    }

    record ForgotPasswordRequest(@NotBlank @Email @Size(max = 254) String email) {}

    record ResetPasswordRequest(
            @NotBlank @Size(max = 256) String token,
            @NotBlank @Size(min = 12, max = 72) String password
    ) {
        @JsonIgnore
        @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes")
        public boolean isPasswordWithinByteLimit() {
            return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }
    }
}
