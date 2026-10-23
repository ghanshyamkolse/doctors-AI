package com.scribe.service;

import com.scribe.dto.SoapExtractionResult;

public interface LLMExtractionService {

    SoapExtractionResult extract(String rawTranscript);
}
