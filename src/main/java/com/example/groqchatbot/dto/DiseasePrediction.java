package com.example.groqchatbot.dto;

import lombok.Data;

import java.util.List;

@Data
public class DiseasePrediction {
    private String disease;
    private int possibility; // 0-100
    private String severity; // low, medium, high, critical
    private String description;
    private List<String> commonSymptoms;
    private List<String> riskFactors;
    private List<String> suggestedMedicines;
    private String advice;
    private String whenToSeeDoctor;
}
