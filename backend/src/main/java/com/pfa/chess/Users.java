package com.pfa.chess;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table
public class Users {
	@Id
	@GeneratedValue
	Integer id;

	String username;
	@Column(unique = true)
	String loginId;
	String password;
	Integer elo;
	List<Integer> matchHistory;

	public Integer getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getLoginId() {
		return loginId;
	}

	public String getPassword() {
		return password;
	}

	public Integer getElo() {
		return elo;
	}

	public List<Integer> getMatchHistory() {
		return matchHistory;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setElo(Integer elo) {
		this.elo = elo;
	}

	public void setMatchHistory(List<Integer> matchHistory) {
		this.matchHistory = matchHistory;
	}

}
