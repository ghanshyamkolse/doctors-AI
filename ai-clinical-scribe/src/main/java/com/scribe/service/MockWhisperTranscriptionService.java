package com.scribe.service;

import com.scribe.config.AppProperties;
import com.scribe.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.whisper.provider", havingValue = "mock", matchIfMissing = true)
public class MockWhisperTranscriptionService implements TranscriptionService {

    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("mp3", "wav", "m4a", "webm", "ogg", "flac", "aac");

    private static final String MOCK_TRANSCRIPT = """
            Doctor: Good morning, what brings you in today?
            Patient: I have had a sore throat, cough, and fever for three days.
            Doctor: Your temperature is 38.4 C, your throat is red, and your lungs are clear on examination.
            Doctor: It looks like acute pharyngitis, likely viral.
            Doctor: I will prescribe amoxicillin 500 mg three times daily for 7 days. Take paracetamol 500 mg as needed for 3 days.
            Doctor: Follow up in one week if symptoms persist or worsen.
            """;

    private final AppProperties appProperties;

    public MockWhisperTranscriptionService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String transcribe(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new BadRequestException("audio file is required and must not be empty");
        }

        validateAudio(audio);
        log.info("Mock Whisper transcription generated for file '{}' ({} bytes)",
                audio.getOriginalFilename(), audio.getSize());
        return MOCK_TRANSCRIPT.trim();
    }

    private void validateAudio(MultipartFile audio) {
        String contentType = audio.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            return;
        }

        String filename = audio.getOriginalFilename();
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        String extension = dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(
                    "unsupported audio format; supported extensions: " + SUPPORTED_EXTENSIONS);
        }
    }
}
