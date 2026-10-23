package com.scribe.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consultations", indexes = {
        @Index(name = "idx_consultations_patient", columnList = "patient_id"),
        @Index(name = "idx_consultations_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "raw_transcript", nullable = false, columnDefinition = "TEXT")
    private String rawTranscript;

    @Column(name = "subjective", columnDefinition = "TEXT")
    private String subjective;

    @Column(name = "objective", columnDefinition = "TEXT")
    private String objective;

    @Column(name = "assessment", columnDefinition = "TEXT")
    private String assessment;

    @Column(name = "plan", columnDefinition = "TEXT")
    private String plan;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<PrescriptionItemEntity> prescriptionItems = new ArrayList<>();

    public void addPrescriptionItem(PrescriptionItemEntity item) {
        if (item == null) {
            return;
        }
        if (prescriptionItems == null) {
            prescriptionItems = new ArrayList<>();
        }
        prescriptionItems.add(item);
        item.setConsultation(this);
    }

    public void clearPrescriptionItems() {
        for (PrescriptionItemEntity item : prescriptionItems) {
            item.setConsultation(null);
        }
        prescriptionItems.clear();
    }
}
