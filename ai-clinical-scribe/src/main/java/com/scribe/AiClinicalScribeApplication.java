package com.scribe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiClinicalScribeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiClinicalScribeApplication.class, args);
    }
}
