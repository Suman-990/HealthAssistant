package com.example.groqchatbot.controller;

import com.example.groqchatbot.dto.PredictionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    @Value("${spring.groq.api.key}")
    private String API_KEY;

    private final String PREDICTION_SYSTEM_PROMPT = """
        You are an advanced AI-powered medical symptom analyzer. Your task is to carefully analyze the user's description of symptoms and provide a structured diagnostic assessment.
        
        Steps:
        1. Extract all mentioned or implied symptoms clearly.
        2. Suggest possible diseases based on the symptoms and provide minimum 1 & maximum 5 diseases.
        3. For each disease, provide:
           - A realistic possibility percentage (0-100%)
           - Severity level: low, medium, high, or critical
           - Brief description of the condition
           - List of common symptoms associated with it
           - Known risk factors
           - Suggested over-the-counter or common medicines (if applicable)
           - Lifestyle/home advice
           - Clear warning signs when to urgently see a doctor

        ALWAYS include a disclaimer.
        
        Respond EXCLUSIVELY in valid JSON format with no additional text, markdown, or explanations outside the JSON:
        
        {
          "extractedSymptoms": ["fever", "cough", "fatigue"],
          "predictions": [
            {
              "disease": "Common Cold",
              "possibility": 85,
              "severity": "low",
              "description": "A viral infection of the upper respiratory tract...",
              "commonSymptoms": ["runny nose", "sore throat", "cough"],
              "riskFactors": ["exposure to virus", "weakened immune system"],
              "suggestedMedicines": ["Paracetamol", "Decongestants", "Vitamin C"],
              "advice": "Rest, stay hydrated, use saline nasal drops...",
              "whenToSeeDoctor": "If fever lasts >3 days or symptoms worsen"
            }
          ],
          "disclaimer": "This tool provides general information and is not a substitute for professional medical diagnosis. Always consult a qualified healthcare provider for proper evaluation and treatment."
        }
        """;

    private final String CHAT_SYSTEM_PROMPT = """
        You are a knowledgeable and empathetic health assistant. Provide accurate, helpful answers to health-related questions.
        Use clear language, explain medical terms when needed, and always encourage consulting a doctor for personal medical concerns.
        Include a disclaimer when appropriate: 'This is general information and not a substitute for professional medical advice.'
        """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/getresponse")
    public PredictionResponse getResponse(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");

        if (userMessage == null || userMessage.trim().isEmpty()) {
            PredictionResponse error = new PredictionResponse();
            error.setDisclaimer("Error: Message cannot be empty.");
            return error;
        }

        try {
            String aiResponse = callGroqWithSystemPrompt(PREDICTION_SYSTEM_PROMPT, userMessage);
            return objectMapper.readValue(aiResponse, PredictionResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            PredictionResponse fallback = new PredictionResponse();
            fallback.setDisclaimer("Sorry, unable to process your request at this time. Please try again later.");
            return fallback;
        }
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Collections.singletonMap("response", "Error: Message cannot be empty");
        }

        try {
            String aiResponse = callGroqWithSystemPrompt(CHAT_SYSTEM_PROMPT, userMessage);
            return Collections.singletonMap("response", aiResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonMap("response", "Sorry, I couldn't process your request right now.");
        }
    }

    private String callGroqWithSystemPrompt(String systemPrompt, String userMessage) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.3-70b-versatile");
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 1500);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();

        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }

        throw new RuntimeException("Invalid response from AI API");
    }
}