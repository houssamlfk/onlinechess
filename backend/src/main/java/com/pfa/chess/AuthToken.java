package com.pfa.chess;

import jakarta.persistence.Entity;

public class AuthToken {
	String loginId;
	String password;

	public String getPassword() {
		return password;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}
}
