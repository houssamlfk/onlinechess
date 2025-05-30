package com.pfa.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.lang.Math;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javafx.util.Pair;

@Service
public class MatchMaker {
	MatchesRepository matchRepo;
	QueueRepository queueRepo;

	public MatchMaker(MatchesRepository matchRepo, QueueRepository queueRepo) {
		this.matchRepo = matchRepo;
		this.queueRepo = queueRepo;
	}

	@Scheduled(fixedRate = 3000)
	public void createMatch() {
		List<PlayerInQueue> players = queueRepo.findAll();
		if (players.size() == 0 || players.size() == 1) {
			return;
		} else {
			PlayerInQueue player1 = players.get(0);
			for (int i = 1; i < players.size(); i++) {
				PlayerInQueue thisPlayer = players.get(i);
				if (Math.abs(thisPlayer.getElo() - player1.getElo()) < 50) {
					Matches thisMatch = new Matches();
					List<String> moves = new ArrayList<String>();
					thisMatch.setMatchDescription(moves);
					thisMatch.setCurrentlyPlayed(true);
					Random random = new Random();
					if (random.nextInt(10) % 2 == 0) {
						thisMatch.setWhiteUsername(player1.getUsername());
						thisMatch.setBlackUsername(thisPlayer.getUsername());
					} else {
						thisMatch.setWhiteUsername(thisPlayer.getUsername());
						thisMatch.setBlackUsername(player1.getUsername());
					}
					queueRepo.deleteById(player1.getId());
					queueRepo.deleteById(thisPlayer.getId());
					matchRepo.save(thisMatch);
					return;
				}
			}
		}
		return;
	}

}
