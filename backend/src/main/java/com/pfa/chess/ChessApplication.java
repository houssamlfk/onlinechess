package com.pfa.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChessApplication {
	public static Boolean loggedIn = false;

	public static void main(String[] args) {
		SpringApplication.run(ChessApplication.class, args);
	}
}
