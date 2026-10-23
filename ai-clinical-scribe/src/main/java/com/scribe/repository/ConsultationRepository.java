package com.scribe.repository;

import com.scribe.model.ConsultationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<ConsultationEntity, Long> {

    boolean existsByPatientId(Long patientId);

    @EntityGraph(attributePaths = "prescriptionItems")
    List<ConsultationEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    @EntityGraph(attributePaths = "prescriptionItems")
    List<ConsultationEntity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "prescriptionItems")
    Optional<ConsultationEntity> findWithPrescriptionItemsById(Long id);
}
