package com.academicpassport.marksheet.provider;

import com.academicpassport.marksheet.dto.ExtractionContext;
import com.academicpassport.marksheet.dto.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GeminiExtractionProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private GeminiExtractionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new GeminiExtractionProvider(restTemplate, "gemini-3.5-flash", "test-key");
    }

    @Test
    void testSuccessfulExtraction() {
        Resource document = new ByteArrayResource("dummy content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.pdf";
            }
        };

        ExtractionContext context = ExtractionContext.builder()
                .expectedStudentName("John Doe")
                .build();

        String jsonResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\n" +
                "            \"text\": \"```json\\n{\\\"studentName\\\": \\\"John Doe\\\"}\\n```\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.exchange(
                ArgumentMatchers.contains("generativelanguage.googleapis.com"),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(jsonResponse));

        ExtractionResult result = provider.extract(document, context);
        assertNotNull(result);
        assertEquals("John Doe", result.getStudentName());
    }

    @Test
    void testClientError_RateLimit_IsRetryable() {
        Resource document = new ByteArrayResource("dummy content".getBytes());
        ExtractionContext context = ExtractionContext.builder().build();

        HttpClientErrorException rateLimitException = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Rate Limited", null, null, null);

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(), eq(String.class)
        )).thenThrow(rateLimitException);

        ExtractionException ex = assertThrows(ExtractionException.class, () -> provider.extract(document, context));
        assertFalse(ex.isPermanent(), "Rate limits (429) should be retryable (not permanent)");
    }

    @Test
    void testClientError_BadRequest_IsPermanent() {
        Resource document = new ByteArrayResource("dummy content".getBytes());
        ExtractionContext context = ExtractionContext.builder().build();

        HttpClientErrorException badRequestException = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null, null, null);

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(), eq(String.class)
        )).thenThrow(badRequestException);

        ExtractionException ex = assertThrows(ExtractionException.class, () -> provider.extract(document, context));
        assertTrue(ex.isPermanent(), "Client errors like 400 Bad Request should be permanent");
    }

    @Test
    void testServerError_IsRetryable() {
        Resource document = new ByteArrayResource("dummy content".getBytes());
        ExtractionContext context = ExtractionContext.builder().build();

        HttpServerErrorException serverErrorException = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", null, null, null);

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(), eq(String.class)
        )).thenThrow(serverErrorException);

        ExtractionException ex = assertThrows(ExtractionException.class, () -> provider.extract(document, context));
        assertFalse(ex.isPermanent(), "Server errors should be retryable");
    }

    @Test
    void testMalformedResponse_IsPermanent() {
        Resource document = new ByteArrayResource("dummy content".getBytes());
        ExtractionContext context = ExtractionContext.builder().build();

        String malformedJsonResponse = "invalid json";

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(), eq(String.class)
        )).thenReturn(ResponseEntity.ok(malformedJsonResponse));

        ExtractionException ex = assertThrows(ExtractionException.class, () -> provider.extract(document, context));
        assertTrue(ex.isPermanent(), "Parsing errors of invalid JSON should be permanent");
    }
}
