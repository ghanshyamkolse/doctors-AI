package com.scribe.dto;

import com.scribe.model.Gender;
import com.scribe.model.PatientEntity;

import java.time.Instant;

public record PatientResponse(
        Long id,
        String name,
        Integer age,
        Gender gender,
        String contactNumber,
        String medicalHistory,
        Instant createdAt
) {

    public static PatientResponse from(PatientEntity entity) {
        return new PatientResponse(
                entity.getId(),
                entity.getName(),
                entity.getAge(),
                entity.getGender(),
                entity.getContactNumber(),
                entity.getMedicalHistory(),
                entity.getCreatedAt()
        );
    }
}
