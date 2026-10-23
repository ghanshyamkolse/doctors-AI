package com.scribe.controller;

import com.scribe.dto.ApiResponse;
import com.scribe.dto.PatientRequest;
import com.scribe.dto.PatientResponse;
import com.scribe.service.PatientService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientResponse>> create(@Valid @RequestBody PatientRequest request) {
        PatientResponse created = patientService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success("Patient created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("Patients retrieved successfully",
                patientService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> findById(
            @PathVariable @Positive(message = "id must be greater than 0") Long id) {
        return ResponseEntity.ok(ApiResponse.success("Patient retrieved successfully",
                patientService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> update(
            @PathVariable @Positive(message = "id must be greater than 0") Long id,
            @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully",
                patientService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable @Positive(message = "id must be greater than 0") Long id) {
        patientService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>success("Patient deleted successfully", null));
    }
}
