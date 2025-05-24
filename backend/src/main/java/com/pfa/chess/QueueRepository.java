package com.pfa.chess;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueRepository extends JpaRepository<PlayerInQueue, Integer> {

	// public PlayerInQueue findOneBySinceGreaterThanEqualUntilLessThanEqual(Integer
	// since, Integer until);

}
