package com.scribe.dto;

import com.scribe.model.PrescriptionItemEntity;

public record PrescriptionItemResponse(
        Long id,
        Long consultationId,
        String medicineName,
        String dosage,
        String frequency,
        String duration
) {

    public static PrescriptionItemResponse from(PrescriptionItemEntity entity) {
        return new PrescriptionItemResponse(
                entity.getId(),
                entity.getConsultationId(),
                entity.getMedicineName(),
                entity.getDosage(),
                entity.getFrequency(),
                entity.getDuration()
        );
    }
}
