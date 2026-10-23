package com.scribe.service;

import org.springframework.web.multipart.MultipartFile;

public interface TranscriptionService {

    String transcribe(MultipartFile audio);
}
