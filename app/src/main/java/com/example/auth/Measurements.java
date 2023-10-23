package com.example.auth;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Measurements {
	public String sessionId;
	public Tetno tetno;
	public Glukoza glukoza;

	public void setSessionId(String sessionId) {this.sessionId = sessionId;}

	public void setTetno(Tetno tetno){
		this.tetno = tetno;
	}

	public void setGlukoza(Glukoza glukoza){
		this.glukoza = glukoza;
	}
}
