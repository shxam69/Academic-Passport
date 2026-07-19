package com.academicpassport.marksheet.provider;

import com.academicpassport.marksheet.dto.ExtractionContext;
import com.academicpassport.marksheet.dto.ExtractionResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeminiExtractionProvider implements MarksheetExtractionProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiExtractionProvider.class);
    private static final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final RestTemplate restTemplate;
    private final String model;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public GeminiExtractionProvider(RestTemplate restTemplate, String model, String apiKey) {
        this.restTemplate = restTemplate;
        this.model = model;
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ExtractionResult extract(Resource document, ExtractionContext context) throws ExtractionException {
        try {
            String mimeType = determineMimeType(document.getFilename());
            String base64Data = Base64.getEncoder().encodeToString(document.getContentAsByteArray());
            
            String prompt = buildPrompt(context);
            Map<String, Object> requestBody = buildRequestBody(prompt, mimeType, base64Data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            String url = String.format(API_URL_TEMPLATE, model, apiKey);

            log.info("Sending document to Gemini API (model: {})", model);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            return parseResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("Gemini API client error: {}", e.getResponseBodyAsString(), e);
            boolean permanent = e.getStatusCode().is4xxClientError() && e.getStatusCode().value() != 429;
            throw new ExtractionException("Client error from Gemini API: " + e.getStatusCode(), e, permanent);
        } catch (HttpServerErrorException e) {
            log.error("Gemini API server error: {}", e.getResponseBodyAsString(), e);
            throw new ExtractionException("Server error from Gemini API: " + e.getStatusCode(), e, false);
        } catch (RestClientException | IOException e) {
            log.error("Network or IO error communicating with Gemini API", e);
            throw new ExtractionException("Communication error with Gemini API", e, false);
        } catch (Exception e) {
            log.error("Unexpected error parsing Gemini response", e);
            throw new ExtractionException("Failed to process Gemini extraction result", e, true);
        }
    }

    private String determineMimeType(String filename) {
        if (filename == null) return "application/pdf";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        return "application/pdf"; // default fallback
    }

    private String buildPrompt(ExtractionContext context) {
        return "You are an expert academic document data extractor. Extract the academic marksheet data from the provided document into a strict JSON format.\n"
                + "Do NOT include Markdown formatting (like ```json). ONLY output the raw JSON object.\n"
                + "The required JSON schema is:\n"
                + "{\n"
                + "  \"studentName\": \"string\",\n"
                + "  \"registerNumber\": \"string\",\n"
                + "  \"semesterNumber\": integer,\n"
                + "  \"institutionName\": \"string\",\n"
                + "  \"subjects\": [\n"
                + "    {\n"
                + "      \"subjectCode\": \"string\",\n"
                + "      \"subjectName\": \"string\",\n"
                + "      \"marksObtained\": integer (null if missing/unreadable),\n"
                + "      \"grade\": \"string\" (null if missing),\n"
                + "      \"totalMarks\": integer\n"
                + "    }\n"
                + "  ]\n"
                + "}\n"
                + "If context is provided, you may use it to guide your extraction, but trust the document first.\n"
                + "Context:\n"
                + "Expected Student Name: " + context.getExpectedStudentName() + "\n"
                + "Expected Register Number: " + context.getExpectedRegisterNumber() + "\n"
                + "Expected Semester: " + context.getExpectedSemesterNumber() + "\n"
                + "Expected Institution: " + context.getExpectedInstitutionName();
    }

    private Map<String, Object> buildRequestBody(String prompt, String mimeType, String base64Data) {
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Data);

        Map<String, Object> inlineDataPart = new HashMap<>();
        inlineDataPart.put("inlineData", inlineData);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart, inlineDataPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        
        return requestBody;
    }

    private ExtractionResult parseResponse(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        
        JsonNode candidates = rootNode.path("candidates");
        if (candidates.isMissingNode() || !candidates.isArray() || candidates.size() == 0) {
            throw new Exception("No candidates returned from Gemini API");
        }

        JsonNode content = candidates.get(0).path("content");
        JsonNode parts = content.path("parts");
        if (parts.isMissingNode() || !parts.isArray() || parts.size() == 0) {
            throw new Exception("No parts returned in Gemini candidate");
        }

        String extractedText = parts.get(0).path("text").asText();
        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new Exception("Extracted text is empty");
        }
        
        // Clean up markdown if Gemini decides to include it despite instructions
        extractedText = extractedText.trim();
        if (extractedText.startsWith("```json")) {
            extractedText = extractedText.substring(7);
        } else if (extractedText.startsWith("```")) {
            extractedText = extractedText.substring(3);
        }
        if (extractedText.endsWith("```")) {
            extractedText = extractedText.substring(0, extractedText.length() - 3);
        }

        return objectMapper.readValue(extractedText.trim(), ExtractionResult.class);
    }
}
