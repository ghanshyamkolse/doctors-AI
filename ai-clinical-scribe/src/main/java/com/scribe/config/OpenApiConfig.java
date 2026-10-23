package com.scribe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI clinicalScribeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AI Clinical Scribe API")
                .version("v1")
                .description("AI-powered clinical scribe and voice assistant backend. "
                        + "Accepts consultation audio or transcripts and produces structured SOAP notes "
                        + "and prescription items.")
                .contact(new Contact().name("Clinical Scribe Platform Team"))
                .license(new License().name("Proprietary")));
    }
}
