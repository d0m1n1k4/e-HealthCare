package com.example.auth;

public class MeasurementSession {
    private String sessionId;

    private String sessionName;

    public MeasurementSession() {
    }

    public MeasurementSession(String sessionId, String sessionName) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    @Override
    public String toString() {
        return sessionName;
    }
}
