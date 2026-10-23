package com.scribe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionItemRequest(

        @NotBlank(message = "medicineName is required")
        @Size(max = 160, message = "medicineName must be at most 160 characters")
        String medicineName,

        @NotBlank(message = "dosage is required")
        @Size(max = 80, message = "dosage must be at most 80 characters")
        String dosage,

        @NotBlank(message = "frequency is required")
        @Size(max = 80, message = "frequency must be at most 80 characters")
        String frequency,

        @NotBlank(message = "duration is required")
        @Size(max = 80, message = "duration must be at most 80 characters")
        String duration
) {
}
