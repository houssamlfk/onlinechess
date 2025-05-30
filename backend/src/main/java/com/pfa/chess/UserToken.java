package com.pfa.chess;

import jakarta.persistence.Entity;

public class UserToken {
	String response;
	Integer elo;

	public String getResponse() {
		return response;
	}

	public Integer getElo() {
		return elo;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public void setElo(Integer elo) {
		this.elo = elo;
	}
}
