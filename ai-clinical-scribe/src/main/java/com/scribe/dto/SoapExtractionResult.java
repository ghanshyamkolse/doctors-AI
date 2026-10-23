package com.scribe.dto;

import java.util.List;

public record SoapExtractionResult(
        String subjective,
        String objective,
        String assessment,
        String plan,
        List<ExtractedMedication> medications
) {
}
