package com.pfa.chess;

public class MatchToken {
	String matchId;
	String opponentName;
	String whiteName;
	String username;
	String opponentElo;
	String playerElo;

	public String getMatchId() {
		return matchId;
	}

	public void setMatchId(String matchId) {
		this.matchId = matchId;
	}

	public String getOpponentName() {
		return opponentName;
	}

	public void setOpponentName(String opponentName) {
		this.opponentName = opponentName;
	}

	public String getWhiteName() {
		return whiteName;
	}

	public void setWhiteName(String whiteName) {
		this.whiteName = whiteName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPlayerElo() {
		return playerElo;
	}

	public void setPlayerElo(String playerElo) {
		this.playerElo = playerElo;
	}

	public String getOpponentElo() {
		return opponentElo;
	}

	public void setOpponentElo(String opponentElo) {
		this.opponentElo = opponentElo;
	}
}
