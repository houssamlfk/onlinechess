package com.pfa.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChessApplication {
	public static Boolean loggedIn = false;

	public static void main(String[] args) {
		SpringApplication.run(ChessApplication.class, args);
	}
}
