package com.abreha.government_benefits.application;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")

public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse submit(@Valid @RequestBody ApplicationRequest request) {
        return applicationService.submit(request.getUserId(), request);
    }

    @GetMapping
    public List<ApplicationResponse> getAll() {
        return applicationService.getAll();
    }

    @GetMapping("/{id}")
    public ApplicationResponse getById(@PathVariable Long id) {
        return applicationService.getById(id);
    }

    @PutMapping("/{id}/status")
    public ApplicationResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusRequest request
    ) {
        return applicationService.updateStatus(id, request.getStatus());
    }

    @GetMapping("/mine")
    public List<ApplicationResponse> getMyApplications(
            java.security.Principal principal
    ) {
        return applicationService.getMyApplications(
                principal.getName()
        );
    }

    @GetMapping("/mine/{id}")
    public ApplicationResponse getMyApplicationById(
            @PathVariable Long id,
            java.security.Principal principal
    ) {
        return applicationService.getMyApplicationById(
                id,
                principal.getName()
        );
    }

    @PostMapping("/mine")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse submitMyApplication(
            @Valid @RequestBody ApplicantApplicationRequest request,
            java.security.Principal principal
    ) {
        return applicationService.submitMyApplication(
                principal.getName(),
                request
        );
    }
}
