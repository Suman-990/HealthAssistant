package com.example.groqchatbot.dto;

import lombok.Data;

import java.util.List;
@Data
public class PredictionResponse {
    private List<String> extractedSymptoms;
    private List<DiseasePrediction> predictions;
    private String disclaimer;
}
