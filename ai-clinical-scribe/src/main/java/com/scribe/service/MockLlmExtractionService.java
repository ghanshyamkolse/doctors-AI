package com.scribe.service;

import com.scribe.config.AppProperties;
import com.scribe.dto.ExtractedMedication;
import com.scribe.dto.SoapExtractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmExtractionService implements LLMExtractionService {

    private static final List<String> OBJECTIVE_KEYWORDS = List.of(
            "temperature", "blood pressure", " bp ", "heart rate", "pulse", "oxygen",
            "saturation", "spo2", "examination", "exam", "auscultation", "tender",
            "vitals", "observed", "measured", "throat is red", "rash", "swollen",
            "wheezing", "murmur", "ecg", "weight", "bmi"
    );

    private static final List<String> ASSESSMENT_KEYWORDS = List.of(
            "diagnosis", "diagnosed", "impression", "likely", "consistent with",
            "suspected", "suspicious", "assessment", "looks like", "suggestive of",
            "differential", "condition"
    );

    private static final List<String> PLAN_KEYWORDS = List.of(
            "prescribe", "prescribed", "medication", "take ", "start", "follow up",
            "follow-up", "return", "advise", "refer", "recommend", "lab", "test",
            "x-ray", "schedule", "order", "administer", "give ", "dose", "physiotherapy",
            "admit"
    );

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+|\\R+");
    private static final Pattern SPEAKER_PREFIX = Pattern.compile(
            "(?i)^(doctor|patient|dr\\.?|clinician|nurse|attending)\\s*:\\s*");
    private static final Pattern MEDICATION_PATTERN = Pattern.compile(
            "(?i)(?:prescribe(?:d)?|take|start|give|administer)\\s+"
                    + "(?:(?:a|an|the)\\s+)?(?:(?:tablet|capsule|syrup|injection)\\s+)?"
                    + "([A-Za-z][A-Za-z0-9-]*)\\s+"
                    + "(\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|g|ml))\\s+"
                    + "([A-Za-z][A-Za-z\\s-]*?)\\s+"
                    + "for\\s+(\\d+\\s*(?:days?|weeks?|months?))"
    );

    private final AppProperties appProperties;

    public MockLlmExtractionService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public SoapExtractionResult extract(String rawTranscript) {
        String transcript = rawTranscript == null ? "" : rawTranscript.trim();
        StringBuilder subjective = new StringBuilder();
        StringBuilder objective = new StringBuilder();
        StringBuilder assessment = new StringBuilder();
        StringBuilder plan = new StringBuilder();

        for (String sentence : splitSentences(transcript)) {
            String cleaned = SPEAKER_PREFIX.matcher(sentence).replaceFirst("").trim();
            if (cleaned.isEmpty()) {
                continue;
            }

            switch (classify(cleaned)) {
                case OBJECTIVE -> append(objective, cleaned);
                case ASSESSMENT -> append(assessment, cleaned);
                case PLAN -> append(plan, cleaned);
                default -> append(subjective, cleaned);
            }
        }

        List<ExtractedMedication> medications = extractMedications(transcript);

        SoapExtractionResult result = new SoapExtractionResult(
                fallback(subjective.toString(), "No subjective findings documented."),
                fallback(objective.toString(), "No objective findings documented."),
                fallback(assessment.toString(), "Assessment pending clinician review."),
                fallback(plan.toString(), "No plan documented."),
                medications
        );

        log.info("Mock SOAP extraction completed using model '{}' ({} medications detected)",
                modelName(), medications.size());
        return result;
    }

    private String modelName() {
        if (appProperties.ai() == null || appProperties.ai().model() == null) {
            return "mock-soap-extractor-v1";
        }
        return appProperties.ai().model();
    }

    private List<String> splitSentences(String transcript) {
        if (transcript.isBlank()) {
            return List.of();
        }
        return List.of(SENTENCE_BOUNDARY.split(transcript)).stream()
                .map(String::trim)
                .filter(sentence -> !sentence.isEmpty())
                .toList();
    }

    private Section classify(String sentence) {
        String lower = " " + sentence.toLowerCase() + " ";

        if (containsAny(lower, OBJECTIVE_KEYWORDS)) {
            return Section.OBJECTIVE;
        }
        if (containsAny(lower, PLAN_KEYWORDS) || MEDICATION_PATTERN.matcher(sentence).find()) {
            return Section.PLAN;
        }
        if (containsAny(lower, ASSESSMENT_KEYWORDS)) {
            return Section.ASSESSMENT;
        }
        return Section.SUBJECTIVE;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void append(StringBuilder target, String sentence) {
        if (target.length() > 0) {
            target.append(' ');
        }
        target.append(sentence);
    }

    private String fallback(String value, String defaultValue) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private List<ExtractedMedication> extractMedications(String transcript) {
        Matcher matcher = MEDICATION_PATTERN.matcher(transcript);
        Map<String, ExtractedMedication> unique = new LinkedHashMap<>();

        while (matcher.find()) {
            ExtractedMedication medication = new ExtractedMedication(
                    normalize(matcher.group(1)),
                    normalize(matcher.group(2)),
                    normalize(matcher.group(3)),
                    normalize(matcher.group(4))
            );
            unique.putIfAbsent(
                    (medication.medicineName() + "|" + medication.dosage()).toLowerCase(),
                    medication
            );
        }

        return new ArrayList<>(unique.values());
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private enum Section {
        SUBJECTIVE,
        OBJECTIVE,
        ASSESSMENT,
        PLAN
    }
}
