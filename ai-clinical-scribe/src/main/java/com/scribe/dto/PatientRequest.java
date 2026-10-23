package com.scribe.dto;

import com.scribe.model.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatientRequest(

        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @NotNull(message = "age is required")
        @Min(value = 0, message = "age must be greater than or equal to 0")
        @Max(value = 150, message = "age must be less than or equal to 150")
        Integer age,

        @NotNull(message = "gender is required")
        Gender gender,

        @NotBlank(message = "contactNumber is required")
        @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,31}$",
                message = "contactNumber must contain 7 to 31 digits and may include +, spaces, hyphens and parentheses")
        String contactNumber,

        @Size(max = 10000, message = "medicalHistory must be at most 10000 characters")
        String medicalHistory
) {
}
