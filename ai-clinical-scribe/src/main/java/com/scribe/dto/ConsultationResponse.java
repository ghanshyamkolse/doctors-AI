package com.scribe.dto;

import com.scribe.model.ConsultationEntity;

import java.time.Instant;
import java.util.List;

public record ConsultationResponse(
        Long id,
        Long patientId,
        String rawTranscript,
        SoapNote soapNote,
        List<PrescriptionItemResponse> prescriptionItems,
        Instant createdAt
) {

    public record SoapNote(
            String subjective,
            String objective,
            String assessment,
            String plan
    ) {
    }

    public static ConsultationResponse from(ConsultationEntity entity) {
        return new ConsultationResponse(
                entity.getId(),
                entity.getPatientId(),
                entity.getRawTranscript(),
                new SoapNote(
                        entity.getSubjective(),
                        entity.getObjective(),
                        entity.getAssessment(),
                        entity.getPlan()
                ),
                entity.getPrescriptionItems().stream()
                        .map(PrescriptionItemResponse::from)
                        .toList(),
                entity.getCreatedAt()
        );
    }
}
