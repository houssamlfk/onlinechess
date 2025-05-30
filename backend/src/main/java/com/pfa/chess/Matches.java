package com.pfa.chess;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import javafx.util.Pair;

@Entity
@Table
public class Matches {
	@Id
	@GeneratedValue
	Integer id;
	List<String> matchDescription;
	String blackUsername;
	String whiteUsername;
	String winner;
	Integer whiteEloChange;
	Integer blackEloChange;
	boolean currentlyPlayed;
	Integer receiptStatus = 2;

	public Integer getId() {
		return id;
	}

	public List<String> getMatchDescription() {
		return matchDescription;
	}

	public String getBlackUsername() {
		return blackUsername;
	}

	public String getWinner() {
		return winner;
	}

	public String getWhiteUsername() {
		return whiteUsername;
	}

	public Integer getWhiteEloChange() {
		return whiteEloChange;
	}

	public Integer getBlackEloChange() {
		return blackEloChange;
	}

	public boolean getCurrentlyPlayed() {
		return currentlyPlayed;
	}

	public Integer getReceiptStatus() {
		return receiptStatus;
	}

	public void setReceiptStatus(Integer receiptStatus) {
		this.receiptStatus = receiptStatus;
	}

	public void setCurrentlyPlayed(boolean currentlyPlayed) {
		this.currentlyPlayed = currentlyPlayed;
	}

	public void setMatchDescription(List<String> matchDescription) {
		this.matchDescription = matchDescription;
	}

	public void setBlackUsername(String blackUsername) {
		this.blackUsername = blackUsername;
	}

	public void setWinner(String winner) {
		this.winner = winner;
	}

	public void setWhiteUsername(String whiteUsername) {
		this.whiteUsername = whiteUsername;
	}

	public void setWhiteEloChange(Integer whiteEloChange) {
		this.whiteEloChange = whiteEloChange;
	}

	public void setBlackEloChange(Integer blackEloChange) {
		this.blackEloChange = blackEloChange;
	}
}
