package com.pfa.chess;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchesRepository extends JpaRepository<Matches, Integer> {

	public Matches findOneByWhiteUsername(String username);

	public List<Matches> findByCurrentlyPlayed(boolean currentlyPlayed);
}
