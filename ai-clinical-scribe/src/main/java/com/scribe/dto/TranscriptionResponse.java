package com.scribe.dto;

public record TranscriptionResponse(
        String provider,
        String fileName,
        String transcript
) {
}
