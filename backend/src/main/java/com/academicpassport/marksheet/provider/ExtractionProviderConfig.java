package com.academicpassport.marksheet.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ExtractionProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(ExtractionProviderConfig.class);

    @Bean
    @ConditionalOnProperty(name = "ai.extraction.provider", havingValue = "gemini")
    public MarksheetExtractionProvider geminiExtractionProvider(
            RestTemplate restTemplate,
            @Value("${ai.gemini.model:gemini-3.5-flash}") String model,
            @Value("${ai.gemini.api-key:}") String apiKey) {
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("AI_GEMINI_API_KEY is missing or empty. Application cannot start in gemini extraction mode.");
            throw new IllegalArgumentException("AI_GEMINI_API_KEY must be provided when AI_EXTRACTION_PROVIDER=gemini");
        }
        
        log.info("Configuring GeminiExtractionProvider with model: {}", model);
        return new GeminiExtractionProvider(restTemplate, model, apiKey);
    }

    @Bean
    @ConditionalOnProperty(name = "ai.extraction.provider", havingValue = "mock", matchIfMissing = true)
    public MarksheetExtractionProvider mockExtractionProvider() {
        log.info("Configuring MockExtractionProvider");
        return new MockExtractionProvider();
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
