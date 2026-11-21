package com.abreha.government_benefits.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI governmentBenefitsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Government Benefits API")
                        .version("1.0")
                        .description(
                                "Registration, benefits, applications, documents, "
                                        + "eligibility, and notifications. "
                                        + "Use Basic Auth for protected endpoints. "
                                        + "For POST, PUT, PATCH, and DELETE requests, "
                                        + "first execute GET /api/auth/csrf and enter "
                                        + "its token in the csrfToken authorization field."
                        )
                )
                .components(new Components()
                        .addSecuritySchemes(
                                "basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description(
                                                "Enter the email and password "
                                                        + "for the role being tested."
                                        )
                        )
                        .addSecuritySchemes(
                                "csrfToken",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-CSRF-TOKEN")
                                        .description(
                                                "Paste the token returned by "
                                                        + "GET /api/auth/csrf "
                                                        + "in this browser session."
                                        )
                        )
                );
    }

    @Bean
    public OpenApiCustomizer endpointSecurityDocumentation() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) ->
                    pathItem.readOperationsMap().forEach((method, operation) -> {
                        boolean publicRead = method == PathItem.HttpMethod.GET
                                && (path.equals("/api/auth/csrf")
                                || path.equals("/api/benefits")
                                || path.equals("/api/benefits/{id}"));

                        boolean registration =
                                method == PathItem.HttpMethod.POST
                                        && path.equals("/api/users/register");

                        boolean requiresCsrf =
                                method == PathItem.HttpMethod.POST
                                        || method == PathItem.HttpMethod.PUT
                                        || method == PathItem.HttpMethod.PATCH
                                        || method == PathItem.HttpMethod.DELETE;

                        SecurityRequirement security =
                                new SecurityRequirement();

                        if (!publicRead && !registration) {
                            security.addList("basicAuth");
                        }

                        if (requiresCsrf) {
                            security.addList("csrfToken");
                        }

                        operation.setSecurity(
                                security.isEmpty()
                                        ? List.of()
                                        : List.of(security)
                        );
                    })
            );
        };
    }
}
