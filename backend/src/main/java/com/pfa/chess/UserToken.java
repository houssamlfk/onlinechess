package com.pfa.chess;

import jakarta.persistence.Entity;

public class UserToken {
	String response;
	Integer Elo;

	public String getResponse() {
		return response;
	}

	public Integer getElo() {
		return Elo;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public void setElo(Integer elo) {
		Elo = elo;
	}
}
