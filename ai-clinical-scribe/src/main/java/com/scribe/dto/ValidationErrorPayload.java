package com.scribe.dto;

import java.util.List;

public record ValidationErrorPayload(String code, List<FieldViolation> violations) {

    public static ValidationErrorPayload of(List<FieldViolation> violations) {
        return new ValidationErrorPayload("VALIDATION_FAILED", violations);
    }
}
