package com.scribe.controller;

import com.scribe.dto.ApiResponse;
import com.scribe.dto.ConsultationRequest;
import com.scribe.dto.ConsultationResponse;
import com.scribe.dto.UpdateConsultationRequest;
import com.scribe.service.ConsultationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConsultationResponse>> create(
            @Valid @RequestBody ConsultationRequest request) {
        ConsultationResponse created = consultationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success("Consultation processed and saved successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsultationResponse>>> findAll(
            @RequestParam(required = false) @Positive(message = "patientId must be greater than 0")
            Long patientId) {
        return ResponseEntity.ok(ApiResponse.success("Consultations retrieved successfully",
                consultationService.findAll(patientId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsultationResponse>> findById(
            @PathVariable @Positive(message = "id must be greater than 0") Long id) {
        return ResponseEntity.ok(ApiResponse.success("Consultation retrieved successfully",
                consultationService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsultationResponse>> update(
            @PathVariable @Positive(message = "id must be greater than 0") Long id,
            @Valid @RequestBody UpdateConsultationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Consultation updated successfully",
                consultationService.update(id, request)));
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<ApiResponse<ConsultationResponse>> reprocess(
            @PathVariable @Positive(message = "id must be greater than 0") Long id) {
        return ResponseEntity.ok(ApiResponse.success("Consultation reprocessed successfully",
                consultationService.reprocess(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable @Positive(message = "id must be greater than 0") Long id) {
        consultationService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>success("Consultation deleted successfully", null));
    }
}
