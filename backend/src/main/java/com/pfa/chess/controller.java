package com.pfa.chess;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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
	String queueUp(@RequestBody String loginId) {
		Users user = new Users();
		user = userRepo.findByLoginId(loginId).get(0);
		PlayerInQueue player = new PlayerInQueue();
		player.setElo(user.getElo());
		player.setUsername(user.getUsername());
		queueRepo.save(player);
		return "player is in queue...";
	}

	@PostMapping("/check-match")
	Integer checkmatch(@RequestBody String loginId) {
		Users user = new Users();
		user = userRepo.findByLoginId(loginId).get(0);
		Matches currentMatch = new Matches();
		List<Matches> matchList = matchRepo.findByCurrentlyPlayed(true);
		for (int i = 0; i < matchList.size(); i++) {
			if (matchList.get(i).getWhiteUsername().compareTo(user.getUsername()) == 0
					|| matchList.get(i).getBlackUsername().compareTo(user.getUsername()) == 0) {
				return matchList.get(i).getId();
			}
		}
		return -1;
	}

	@PostMapping("/match-result")
	UserToken matchResult(@RequestBody String matchId, String username) {
		matchRepo.save(match);
		return "";
	}

	@PostMapping("/{matchId}")
	String moveSystem(@RequestBody moveToken move, @PathVariable("matchId") String matchId) {

	}

	@PostMapping("/signup")
	String signup(@RequestBody SignupToken user) {
		List<Users> potentialUserList = userRepo.findByLoginId(user.getLoginId());
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
		List<Users> potentialUserList = userRepo.findByLoginId(token.getLoginId());
		if (potentialUserList.size() == 0) {
			tokenToSend.setElo(0);
			tokenToSend.setResponse("User is not signed up !");
			return tokenToSend;
		} else {
			Users user = potentialUserList.get(0);
			tokenToSend.setElo(user.getElo());
			if (user.getPassword().compareTo(token.getPassword()) == 0) {
				ChessApplication.loggedIn = true;
				tokenToSend.setResponse("successfully authenticated as " + token.getLoginId() + "!");
				return tokenToSend;
			} else {
				tokenToSend.setResponse("Wrong Credentials!");
				return tokenToSend;
			}
		}

	}
}
