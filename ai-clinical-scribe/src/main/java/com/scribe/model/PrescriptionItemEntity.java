package com.scribe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prescription_items", indexes = {
        @Index(name = "idx_prescription_items_consultation", columnList = "consultation_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private ConsultationEntity consultation;

    @Column(name = "medicine_name", nullable = false, length = 160)
    private String medicineName;

    @Column(name = "dosage", nullable = false, length = 80)
    private String dosage;

    @Column(name = "frequency", nullable = false, length = 80)
    private String frequency;

    @Column(name = "duration", nullable = false, length = 80)
    private String duration;

    public Long getConsultationId() {
        return consultation == null ? null : consultation.getId();
    }
}
