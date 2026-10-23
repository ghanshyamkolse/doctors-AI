package com.scribe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateConsultationRequest(

        @Size(max = 20000, message = "subjective must be at most 20000 characters")
        String subjective,

        @Size(max = 20000, message = "objective must be at most 20000 characters")
        String objective,

        @Size(max = 20000, message = "assessment must be at most 20000 characters")
        String assessment,

        @Size(max = 20000, message = "plan must be at most 20000 characters")
        String plan,

        @Valid
        List<PrescriptionItemRequest> prescriptionItems
) {
}
