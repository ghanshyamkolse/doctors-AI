package com.scribe.service;

import com.scribe.dto.ConsultationRequest;
import com.scribe.dto.ConsultationResponse;
import com.scribe.dto.ExtractedMedication;
import com.scribe.dto.SoapExtractionResult;
import com.scribe.exception.ResourceNotFoundException;
import com.scribe.model.ConsultationEntity;
import com.scribe.repository.ConsultationRepository;
import com.scribe.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private LLMExtractionService llmExtractionService;

    @InjectMocks
    private ConsultationService consultationService;

    @Test
    void createPersistsExtractedSoapNoteAndPrescriptions() {
        ConsultationRequest request = new ConsultationRequest(5L,
                "Patient reports a sore throat and fever; prescribe amoxicillin 500 mg twice daily for 7 days.");

        when(patientRepository.existsById(5L)).thenReturn(true);
        when(llmExtractionService.extract(request.rawTranscript())).thenReturn(new SoapExtractionResult(
                "sore throat and fever",
                "temperature 38.4 C",
                "acute pharyngitis",
                "prescribe amoxicillin",
                List.of(new ExtractedMedication("amoxicillin", "500 mg", "twice daily", "7 days"))
        ));
        when(consultationRepository.save(any(ConsultationEntity.class))).thenAnswer(invocation -> {
            ConsultationEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            entity.setCreatedAt(Instant.now());
            entity.getPrescriptionItems().forEach(item -> item.setId(200L));
            return entity;
        });

        ConsultationResponse response = consultationService.create(request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.patientId()).isEqualTo(5L);
        assertThat(response.soapNote().subjective()).isEqualTo("sore throat and fever");
        assertThat(response.soapNote().assessment()).isEqualTo("acute pharyngitis");
        assertThat(response.prescriptionItems()).hasSize(1);
        assertThat(response.prescriptionItems().get(0).medicineName()).isEqualTo("amoxicillin");
        assertThat(response.prescriptionItems().get(0).consultationId()).isEqualTo(100L);

        verify(llmExtractionService).extract(request.rawTranscript());
        verify(consultationRepository).save(any(ConsultationEntity.class));
    }

    @Test
    void createRejectsUnknownPatientWithoutCallingExtraction() {
        ConsultationRequest request = new ConsultationRequest(999L,
                "Patient reports a headache for two days.");

        when(patientRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> consultationService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient");

        verifyNoInteractions(llmExtractionService);
        verify(consultationRepository, never()).save(any());
    }
}
