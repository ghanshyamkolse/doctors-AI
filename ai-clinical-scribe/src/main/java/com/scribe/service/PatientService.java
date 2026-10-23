package com.scribe.service;

import com.scribe.dto.PatientRequest;
import com.scribe.dto.PatientResponse;
import com.scribe.exception.ConflictException;
import com.scribe.exception.ResourceNotFoundException;
import com.scribe.model.PatientEntity;
import com.scribe.repository.ConsultationRepository;
import com.scribe.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;

    public PatientService(PatientRepository patientRepository,
                          ConsultationRepository consultationRepository) {
        this.patientRepository = patientRepository;
        this.consultationRepository = consultationRepository;
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        PatientEntity patient = PatientEntity.builder()
                .name(request.name().trim())
                .age(request.age())
                .gender(request.gender())
                .contactNumber(request.contactNumber().trim())
                .medicalHistory(request.medicalHistory())
                .build();
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findAll() {
        return patientRepository.findAll().stream()
                .map(PatientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse findById(Long id) {
        return PatientResponse.from(getEntity(id));
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        PatientEntity patient = getEntity(id);
        patient.setName(request.name().trim());
        patient.setAge(request.age());
        patient.setGender(request.gender());
        patient.setContactNumber(request.contactNumber().trim());
        patient.setMedicalHistory(request.medicalHistory());
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public void delete(Long id) {
        PatientEntity patient = getEntity(id);
        if (consultationRepository.existsByPatientId(id)) {
            throw new ConflictException(
                    "patient " + id + " has existing consultations and cannot be deleted");
        }
        patientRepository.delete(patient);
    }

    @Transactional(readOnly = true)
    public PatientEntity getEntity(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
    }
}
