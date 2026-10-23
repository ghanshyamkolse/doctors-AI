package com.scribe.dto;

public record ExtractedMedication(
        String medicineName,
        String dosage,
        String frequency,
        String duration
) {
}
