package com.pfa.chess;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table
public class Matches {
	@Id
	Integer id;
	List<String> matchDescription;
	Integer blackId;
	Integer whiteId;
	String winner;
	Integer whiteEloChange;
	Integer blackEloChange;

	public Integer getId() {
		return id;
	}

	public List<String> getMatchDescription() {
		return matchDescription;
	}

	public Integer getBlackId() {
		return blackId;
	}

	public String getWinner() {
		return winner;
	}

	public Integer getWhiteId() {
		return whiteId;
	}

	public Integer getWhiteEloChange() {
		return whiteEloChange;
	}

	public Integer getBlackEloChange() {
		return blackEloChange;
	}

	public void setMatchDescription(List<String> matchDescription) {
		this.matchDescription = matchDescription;
	}

	public void setBlackId(Integer blackId) {
		this.blackId = blackId;
	}

	public void setWinner(String winner) {
		this.winner = winner;
	}

	public void setWhiteId(Integer whiteId) {
		this.whiteId = whiteId;
	}

	public void setWhiteEloChange(Integer whiteEloChange) {
		this.whiteEloChange = whiteEloChange;
	}

	public void setBlackEloChange(Integer blackEloChange) {
		this.blackEloChange = blackEloChange;
	}
}
