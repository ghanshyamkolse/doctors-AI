package com.scribe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConsultationRequest(

        @NotNull(message = "patientId is required")
        @Positive(message = "patientId must be greater than 0")
        Long patientId,

        @NotBlank(message = "rawTranscript is required")
        @Size(min = 10, max = 50000, message = "rawTranscript must be between 10 and 50000 characters")
        String rawTranscript
) {
}
