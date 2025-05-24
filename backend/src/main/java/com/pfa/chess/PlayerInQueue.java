package com.pfa.chess;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Queue")
public class PlayerInQueue {
	@Id
	@GeneratedValue
	Integer id;
	String username;
	Integer elo;

	public Integer getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public Integer getElo() {
		return elo;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setElo(Integer elo) {
		this.elo = elo;
	}
}
