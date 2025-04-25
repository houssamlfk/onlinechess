package com.pfa.chess;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;

@RestController
class Controller {
	private final MatchesRepository matchRepo;
	private final UsersRepository userRepo;

	public Controller(MatchesRepository matchRepo, UsersRepository userRepo) {
		this.matchRepo = matchRepo;
		this.userRepo = userRepo;
	}

	@PostMapping("/queue")
	String queueup(@RequestBody Users user) {
		// matchmaking algorithm TOBE implemented (it's not that simple)
		return "";
	}

	@PostMapping("/match-result")
	String matchResult(@RequestBody Matches match) {
		matchRepo.save(match);
		return "";
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
	String authenticate(@RequestBody AuthToken token) {
		List<Users> potentialUserList = userRepo.findByLoginId(token.getLoginId());
		if (potentialUserList.size() == 0) {
			return "User is not signed up !";
		} else {
			Users user = potentialUserList.get(0);
			if (user.getPassword().compareTo(token.getPassword()) == 0) {
				ChessApplication.loggedIn = true;
				return "successfully authenticated as " + token.getLoginId() + "!";
			} else {
				return "Wrong Credentials!";
			}
		}

	}
}
