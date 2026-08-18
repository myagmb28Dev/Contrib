package com.example.project.certificate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeCertificateRequest(@NotBlank @Size(max = 1000) String reason) {
}
