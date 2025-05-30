package com.pfa.chess;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.transaction.Transactional;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
class Controller {
	private final MatchesRepository matchRepo;
	private final UsersRepository userRepo;
	private final QueueRepository queueRepo;

	public Controller(MatchesRepository matchRepo, UsersRepository userRepo, QueueRepository queueRepo) {
		this.matchRepo = matchRepo;
		this.userRepo = userRepo;
		this.queueRepo = queueRepo;
	}

	@PostMapping("/queue")
	String queueUp(@RequestBody QueueToken token) {
		Users user = new Users();
		System.out.println(token.getLoginId());
		user = userRepo.findAllByLoginId(token.getLoginId()).get(0);
		PlayerInQueue player = new PlayerInQueue();
		player.setElo(user.getElo());
		player.setUsername(user.getUsername());
		queueRepo.save(player);
		return "player is in queue...";
	}

	@PostMapping("/check-match")
	MatchToken checkmatch(@RequestBody QueueToken token) {
		Users user = new Users();
		MatchToken tokenToSend = new MatchToken();
		user = userRepo.findAllByLoginId(token.getLoginId()).get(0);
		Matches currentMatch = new Matches();
		List<Matches> matchList = matchRepo.findByCurrentlyPlayed(true);
		if (matchList.size() == 0) {
			tokenToSend.setMatchId("-1");
			tokenToSend.setWhiteName("");
			tokenToSend.setOpponentName("");
			return tokenToSend;
		}
		for (int i = 0; i < matchList.size(); i++) {
			if (matchList.get(i).getWhiteUsername().compareTo(user.getUsername()) == 0
					|| matchList.get(i).getBlackUsername().compareTo(user.getUsername()) == 0) {
				tokenToSend.setPlayerElo(user.getElo().toString());
				tokenToSend.setWhiteName(matchList.get(i).getWhiteUsername());
				tokenToSend.setUsername(user.getUsername());
				tokenToSend.setMatchId(matchList.get(i).getId().toString());

				String opponentName = (user.getUsername().equals(matchList.get(i).getWhiteUsername()))
						? matchList.get(i).getBlackUsername()
						: matchList.get(i).getWhiteUsername();
				Users opponent = new Users();
				opponent = userRepo.findAllByUsername(opponentName).get(0);
				String opponentElo = opponent.getElo().toString();
				tokenToSend.setOpponentName(opponentName);
				tokenToSend.setOpponentElo(opponentElo);

				int status = matchList.get(i).getReceiptStatus();
				status -= 1;
				matchList.get(i).setReceiptStatus(status);
				if (status == 0) {
					matchList.get(i).setCurrentlyPlayed(false);
				}
				matchRepo.save(matchList.get(i));
				return tokenToSend;
			}
		}
		tokenToSend.setMatchId("-1");
		tokenToSend.setWhiteName("");
		tokenToSend.setOpponentName("");
		return tokenToSend;
	}

	@GetMapping("/match-result/{matchId}/{color}")
	String matchResult(@PathVariable("matchId") Integer matchId, @PathVariable("color") String color) {
		Matches match = new Matches();
		Users blackUser = new Users();
		Users whiteUser = new Users();

		match = matchRepo.findOneById(matchId);
		blackUser = userRepo.findAllByUsername(match.getBlackUsername()).get(0);
		whiteUser = userRepo.findAllByUsername(match.getWhiteUsername()).get(0);

		int blackElo = blackUser.getElo();
		int whiteElo = whiteUser.getElo();

		double probab = 1.0 / (1 + Math.pow(10, (blackElo - whiteElo) / 400.0));
		double probaw = 1.0 / (1 + Math.pow(10, (whiteElo - blackElo) / 400.0));

		if (color.equals("White")) {
			double whiteEloChange = (1 - probaw) * 10;
			double blackEloChange = (-probab) * 10;
			match.setBlackEloChange((int) blackEloChange);
			match.setWhiteEloChange((int) whiteEloChange);
			whiteUser.setElo(whiteUser.getElo() + (int) whiteEloChange);
			blackUser.setElo(blackUser.getElo() + (int) blackEloChange);
		} else {
			double whiteEloChange = (-probaw) * 10;
			double blackEloChange = (1 - probab) * 10;
			match.setWhiteEloChange((int) whiteEloChange);
			match.setBlackEloChange((int) blackEloChange);
			whiteUser.setElo(whiteUser.getElo() + (int) whiteEloChange);
			blackUser.setElo(blackUser.getElo() + (int) blackEloChange);
		}
		userRepo.save(blackUser);
		userRepo.save(whiteUser);
		matchRepo.save(match);
		return "end match processing is done!";
	}

	@GetMapping("/elo/{username}")
	public String getElo(@PathVariable String username) {
		Integer elo;
		Users user = new Users();
		user = userRepo.findAllByUsername(username).get(0);
		elo = user.getElo();
		return elo.toString();
	}

	@PostMapping("/match/{matchId}/{move}/{username}")
	String moveSystem(@PathVariable("matchId") Integer matchId, @PathVariable("move") String move,
			@PathVariable("username") String username) {
		Matches thisMatch = new Matches();
		thisMatch = matchRepo.findOneById(matchId);
		if (thisMatch == null) {
			return "this match doesn't exist!";
		}
		List<String> newMoves = thisMatch.getMatchDescription();
		if (username.equals(thisMatch.getWhiteUsername())) {
			newMoves.add("1" + move);
		} else {
			newMoves.add("0" + move);
		}
		thisMatch.setMatchDescription(newMoves);
		matchRepo.save(thisMatch);
		return "move \"" + move + "\" has been received successfully!";
	}

	@GetMapping("/match/{matchId}/{username}")
	String moveRequester(@PathVariable("matchId") Integer matchId, @PathVariable("username") String username) {
		Matches thisMatch = new Matches();
		thisMatch = matchRepo.findOneById(matchId);
		List<String> moves = thisMatch.getMatchDescription();
		if (thisMatch.getMatchDescription().size() == 0) {
			return "x";
		}
		String move = moves.get(moves.size() - 1);
		System.out.println("comparing:" + thisMatch.getWhiteUsername() + "," + username);
		if (move.charAt(0) != '0' && move.charAt(0) != '1') {
			return "x";
		}
		if (username.equals(thisMatch.getWhiteUsername())) {
			if (move.charAt(0) == '1') {
				return "x";
			} else {
				System.out.println(move.substring(1));
				moves.remove(moves.size() - 1);
				moves.add(move.substring(1));
				thisMatch.setMatchDescription(moves);
				matchRepo.save(thisMatch);
				return move.substring(1);
			}
		} else {
			if (move.charAt(0) == '0') {
				return "x";
			} else {
				moves.remove(moves.size() - 1);
				moves.add(move.substring(1));
				thisMatch.setMatchDescription(moves);
				matchRepo.save(thisMatch);
				return move.substring(1);
			}
		}
	}

	@PostMapping("/signup")
	String signup(@RequestBody SignupToken user) {
		List<Users> potentialUserList = userRepo.findAllByLoginId(user.getLoginId());
		Users userCreated = new Users();
		userCreated.setElo(800);
		List<Integer> matchHistoryList = new ArrayList<Integer>();
		matchHistoryList.add(0);
		userCreated.setMatchHistory(matchHistoryList);
		userCreated.setLoginId(user.getLoginId());
		userCreated.setPassword(user.getPassword());
		userCreated.setUsername(user.getUsername());
		if (potentialUserList.size() == 0) {
			userRepo.save(userCreated);
			return "user has been successfully signed up ! ";
		} else {
			return "user is already signed up !";
		}
	}

	@PostMapping("/login")
	UserToken authenticate(@RequestBody AuthToken token) {
		UserToken tokenToSend = new UserToken();
		List<Users> potentialUserList = userRepo.findAllByLoginId(token.getLoginId());
		if (potentialUserList.size() == 0) {
			tokenToSend.setElo(0);
			tokenToSend.setResponse("User is not signed up !");
			return tokenToSend;
		} else {
			Users user = potentialUserList.get(0);
			tokenToSend.setElo(user.getElo());
			if (user.getPassword().compareTo(token.getPassword()) == 0) {
				tokenToSend.setResponse("successfully authenticated as " + token.getLoginId() + "!");
				return tokenToSend;
			} else {
				tokenToSend.setResponse("Wrong Credentials!");
				return tokenToSend;
			}
		}

	}
}
