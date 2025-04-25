package com.pfa.chess;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class MatchMovesController {
	@MessageMapping("/move")
	@SendTo("/topic/moves")
	public String moveMade(
			String move) {
		return move;
	}
}
