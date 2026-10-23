package com.scribe.service;

import com.scribe.dto.ConsultationRequest;
import com.scribe.dto.ConsultationResponse;
import com.scribe.dto.ExtractedMedication;
import com.scribe.dto.PrescriptionItemRequest;
import com.scribe.dto.SoapExtractionResult;
import com.scribe.dto.UpdateConsultationRequest;
import com.scribe.exception.ResourceNotFoundException;
import com.scribe.model.ConsultationEntity;
import com.scribe.model.PrescriptionItemEntity;
import com.scribe.repository.ConsultationRepository;
import com.scribe.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final LLMExtractionService llmExtractionService;

    public ConsultationService(ConsultationRepository consultationRepository,
                               PatientRepository patientRepository,
                               LLMExtractionService llmExtractionService) {
        this.consultationRepository = consultationRepository;
        this.patientRepository = patientRepository;
        this.llmExtractionService = llmExtractionService;
    }

    @Transactional
    public ConsultationResponse create(ConsultationRequest request) {
        ensurePatientExists(request.patientId());

        SoapExtractionResult extraction = llmExtractionService.extract(request.rawTranscript());

        ConsultationEntity consultation = ConsultationEntity.builder()
                .patientId(request.patientId())
                .rawTranscript(request.rawTranscript().trim())
                .subjective(extraction.subjective())
                .objective(extraction.objective())
                .assessment(extraction.assessment())
                .plan(extraction.plan())
                .build();

        applyMedications(consultation, extraction.medications());

        ConsultationEntity saved = consultationRepository.save(consultation);
        log.info("Persisted consultation {} for patient {} with {} prescription item(s)",
                saved.getId(), saved.getPatientId(), saved.getPrescriptionItems().size());
        return ConsultationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ConsultationResponse findById(Long id) {
        return ConsultationResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponse> findAll(Long patientId) {
        List<ConsultationEntity> consultations;
        if (patientId == null) {
            consultations = consultationRepository.findAllByOrderByCreatedAtDesc();
        } else {
            ensurePatientExists(patientId);
            consultations = consultationRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        }
        return consultations.stream()
                .map(ConsultationResponse::from)
                .toList();
    }

    @Transactional
    public ConsultationResponse update(Long id, UpdateConsultationRequest request) {
        ConsultationEntity consultation = getEntity(id);

        if (request.subjective() != null) {
            consultation.setSubjective(request.subjective());
        }
        if (request.objective() != null) {
            consultation.setObjective(request.objective());
        }
        if (request.assessment() != null) {
            consultation.setAssessment(request.assessment());
        }
        if (request.plan() != null) {
            consultation.setPlan(request.plan());
        }
        if (request.prescriptionItems() != null) {
            consultation.clearPrescriptionItems();
            for (PrescriptionItemRequest item : request.prescriptionItems()) {
                consultation.addPrescriptionItem(toEntity(item));
            }
        }

        ConsultationEntity saved = consultationRepository.save(consultation);
        log.info("Updated consultation {}", saved.getId());
        return ConsultationResponse.from(saved);
    }

    @Transactional
    public ConsultationResponse reprocess(Long id) {
        ConsultationEntity consultation = getEntity(id);
        SoapExtractionResult extraction = llmExtractionService.extract(consultation.getRawTranscript());

        consultation.setSubjective(extraction.subjective());
        consultation.setObjective(extraction.objective());
        consultation.setAssessment(extraction.assessment());
        consultation.setPlan(extraction.plan());
        consultation.clearPrescriptionItems();
        applyMedications(consultation, extraction.medications());

        ConsultationEntity saved = consultationRepository.save(consultation);
        log.info("Reprocessed consultation {} with {} prescription item(s)",
                saved.getId(), saved.getPrescriptionItems().size());
        return ConsultationResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        ConsultationEntity consultation = getEntity(id);
        consultationRepository.delete(consultation);
        log.info("Deleted consultation {}", id);
    }

    private void applyMedications(ConsultationEntity consultation,
                                  List<ExtractedMedication> medications) {
        if (medications == null) {
            return;
        }
        for (ExtractedMedication medication : medications) {
            consultation.addPrescriptionItem(PrescriptionItemEntity.builder()
                    .medicineName(medication.medicineName())
                    .dosage(medication.dosage())
                    .frequency(medication.frequency())
                    .duration(medication.duration())
                    .build());
        }
    }

    private PrescriptionItemEntity toEntity(PrescriptionItemRequest request) {
        return PrescriptionItemEntity.builder()
                .medicineName(request.medicineName().trim())
                .dosage(request.dosage().trim())
                .frequency(request.frequency().trim())
                .duration(request.duration().trim())
                .build();
    }

    private void ensurePatientExists(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
    }

    private ConsultationEntity getEntity(Long id) {
        return consultationRepository.findWithPrescriptionItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation", id));
    }
}
