package com.pfa.chess;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class CleanupService {

	private final QueueRepository queueRepo;

	public CleanupService(QueueRepository queueRepo) {
		this.queueRepo = queueRepo;
	}

	@PreDestroy
	public void onShutdown() {
		queueRepo.deleteAll();
		System.out.println("Table 'queue' cleared on shutdown.");
	}
}
