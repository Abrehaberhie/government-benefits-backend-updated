package com.abreha.government_benefits.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SecurityConfig {

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher sessionEvents() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityErrorHandler securityErrorHandler,
            SessionRegistry sessionRegistry
    ) throws Exception {
        var browserLoginFailure =
                new SimpleUrlAuthenticationFailureHandler("/login?error");

        http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/benefits",
                                "/api/benefits/*"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/register",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        )
                        .permitAll()

                        .requestMatchers("/api/auth/me")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/applications/mine",
                                "/api/applications/mine/*"
                        )
                        .hasRole("APPLICANT")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/applications/mine"
                        )
                        .hasRole("APPLICANT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/documents/mine/application/*",
                                "/api/documents/mine/*/download"
                        )
                        .hasRole("APPLICANT")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/mine/application/*"
                        )
                        .hasRole("APPLICANT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/notifications/mine",
                                "/api/notifications/mine/unread-count"
                        )
                        .hasRole("APPLICANT")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/notifications/mine/*/read"
                        )
                        .hasRole("APPLICANT")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/eligibility/mine/check"
                        )
                        .hasRole("APPLICANT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/applications",
                                "/api/applications/*"
                        )
                        .hasAnyRole("CASE_WORKER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/applications/*/status"
                        )
                        .hasAnyRole("CASE_WORKER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/documents/application/*",
                                "/api/documents/*/download"
                        )
                        .hasAnyRole("CASE_WORKER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/documents/*/review"
                        )
                        .hasAnyRole("CASE_WORKER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        )
                        .permitAll()

                        .anyRequest()
                        .hasRole("ADMIN")
                )
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor(
                                securityErrorHandler,
                                PathPatternRequestMatcher.withDefaults()
                                        .matcher("/api/**")
                        )
                        .accessDeniedHandler(securityErrorHandler)
                )
                .sessionManagement(session -> session.maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredSessionStrategy(event -> securityErrorHandler.commence(
                                event.getRequest(),
                                event.getResponse(),
                                new InsufficientAuthenticationException("Session expired")
                        )))
                .logout(logout -> logout.logoutSuccessHandler(
                        (request, response, authentication) -> response.setStatus(204)
                ))
                .formLogin(form -> form
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpStatus.SEE_OTHER.value());
                            response.setHeader(
                                    HttpHeaders.LOCATION,
                                    request.getContextPath() + "/api/auth/me"
                            );
                        })
                        .failureHandler((request, response, exception) -> {
                            if (exception instanceof AuthenticationServiceException) {
                                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
                                return;
                            }

                            String accept = request.getHeader(HttpHeaders.ACCEPT);
                            if (accept != null && accept.startsWith(MediaType.TEXT_HTML_VALUE)) {
                                browserLoginFailure.onAuthenticationFailure(
                                        request, response, exception
                                );
                            } else {
                                securityErrorHandler.commence(request, response, exception);
                            }
                        })
                        .permitAll()
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(securityErrorHandler));

        return http.build();
    }
}
