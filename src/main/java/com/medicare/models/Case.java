package com.medicare.models;

import java.util.List;

public class Case {

    private final String patientName;
    private final int patientAge;
    private final String initialComplaint;
    private final List<String> interviewClues; // Clues revealed by "Ask Questions"
    private final List<String> labTestClues;   // Clues revealed by "Run Lab Tests"
    private final String correctDiagnosis;
    private final List<String> distractorDiagnoses; // Incorrect options to make it challenging

    public Case(String patientName, int patientAge, String initialComplaint,
                List<String> interviewClues, List<String> labTestClues,
                String correctDiagnosis, List<String> distractorDiagnoses) {
        this.patientName = patientName;
        this.patientAge = patientAge;
        this.initialComplaint = initialComplaint;
        this.interviewClues = interviewClues;
        this.labTestClues = labTestClues;
        this.correctDiagnosis = correctDiagnosis;
        this.distractorDiagnoses = distractorDiagnoses;
    }

    // --- Getters ---

    public String getPatientName() {
        return patientName;
    }

    public int getPatientAge() {
        return patientAge;
    }

    public String getInitialComplaint() {
        return initialComplaint;
    }

    public List<String> getInterviewClues() {
        return interviewClues;
    }

    public List<String> getLabTestClues() {
        return labTestClues;
    }

    public String getCorrectDiagnosis() {
        return correctDiagnosis;
    }

    public List<String> getDistractorDiagnoses() {
        return distractorDiagnoses;
    }
}
