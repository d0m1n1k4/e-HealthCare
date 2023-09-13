package com.example.auth;
public class Measurement {
    private String measurementName;
    private String tetnoValue;
    private String glukozaValue;

    public Measurement(String measurementName, String tetnoValue, String glukozaValue) {
        this.measurementName = measurementName;
        this.tetnoValue = tetnoValue;
        this.glukozaValue = glukozaValue;
    }

    public String getMeasurementName() {
        return measurementName;
    }

    public void setMeasurementName(String measurementName) {
        this.measurementName = measurementName;
    }

    public String getTetnoValue() {
        return tetnoValue;
    }

    public void setTetnoValue(String tetnoValue) {
        this.tetnoValue = tetnoValue;
    }

    public String getGlukozaValue() {
        return glukozaValue;
    }

    public void setGlukozaValue(String glukozaValue) {
        this.glukozaValue = glukozaValue;
    }
}
