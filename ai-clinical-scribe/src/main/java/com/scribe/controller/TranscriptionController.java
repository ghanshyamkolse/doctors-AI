package com.scribe.controller;

import com.scribe.config.AppProperties;
import com.scribe.dto.ApiResponse;
import com.scribe.dto.TranscriptionResponse;
import com.scribe.service.TranscriptionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/transcriptions")
public class TranscriptionController {

    private final TranscriptionService transcriptionService;
    private final AppProperties appProperties;

    public TranscriptionController(TranscriptionService transcriptionService,
                                   AppProperties appProperties) {
        this.transcriptionService = transcriptionService;
        this.appProperties = appProperties;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TranscriptionResponse>> transcribe(
            @RequestPart("audio") MultipartFile audio) {
        String transcript = transcriptionService.transcribe(audio);
        String provider = appProperties.ai() == null || appProperties.ai().whisper() == null
                ? "mock"
                : appProperties.ai().whisper().provider();

        TranscriptionResponse response = new TranscriptionResponse(
                provider,
                audio.getOriginalFilename(),
                transcript
        );
        return ResponseEntity.ok(ApiResponse.success("Audio transcribed successfully", response));
    }
}
