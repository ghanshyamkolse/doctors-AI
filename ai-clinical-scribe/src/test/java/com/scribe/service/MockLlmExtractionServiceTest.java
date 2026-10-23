package com.scribe.service;

import com.scribe.config.AppProperties;
import com.scribe.dto.SoapExtractionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockLlmExtractionServiceTest {

    private final MockLlmExtractionService service =
            new MockLlmExtractionService(new AppProperties(null, null));

    private final String transcript = """
            Doctor: Good morning, what brings you in today?
            Patient: I have had a sore throat and fever for three days.
            Doctor: Your temperature is 38.4 C and your throat is red.
            Doctor: It looks like acute pharyngitis.
            Doctor: I will prescribe amoxicillin 500 mg three times daily for 7 days.
            Doctor: Follow up in one week if symptoms persist.
            """;

    @Test
    void extractsSoapSectionsFromTranscript() {
        SoapExtractionResult result = service.extract(transcript);

        assertThat(result.subjective()).contains("sore throat", "fever");
        assertThat(result.objective()).contains("temperature", "throat is red");
        assertThat(result.assessment()).contains("pharyngitis");
        assertThat(result.plan()).contains("prescribe", "Follow up");
    }

    @Test
    void extractsPrescriptionItems() {
        SoapExtractionResult result = service.extract(transcript);

        assertThat(result.medications()).hasSize(1);
        assertThat(result.medications().get(0).medicineName()).isEqualTo("amoxicillin");
        assertThat(result.medications().get(0).dosage()).isEqualTo("500 mg");
        assertThat(result.medications().get(0).frequency()).isEqualTo("three times daily");
        assertThat(result.medications().get(0).duration()).isEqualTo("7 days");
    }

    @Test
    void fallsBackToDefaultsWhenTranscriptHasNoClinicalContent() {
        SoapExtractionResult result = service.extract("");

        assertThat(result.subjective()).isEqualTo("No subjective findings documented.");
        assertThat(result.objective()).isEqualTo("No objective findings documented.");
        assertThat(result.assessment()).isEqualTo("Assessment pending clinician review.");
        assertThat(result.plan()).isEqualTo("No plan documented.");
        assertThat(result.medications()).isEmpty();
    }
}
