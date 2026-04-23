package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.service.PlayerService;

@RestController
@RequestMapping("/games/{gameId}/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/{playerId}/surrender")
    @ResponseStatus(HttpStatus.OK)
    public void surrender(
            @PathVariable Long gameId,
            @PathVariable Long playerId) {
        playerService.surrender(gameId, playerId);
    }
}