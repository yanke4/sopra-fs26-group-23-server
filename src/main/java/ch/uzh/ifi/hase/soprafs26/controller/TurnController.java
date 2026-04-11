package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnAttackDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnDeployDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnMoveDTO;
import ch.uzh.ifi.hase.soprafs26.service.TurnService;

@RestController
@RequestMapping("/games/{gameId}/turns")
public class TurnController {

    private final TurnService turnService;

    public TurnController(TurnService turnService) {
        this.turnService = turnService;
    }

    @PostMapping("/deploy")
    @ResponseStatus(HttpStatus.OK)
    public void deploy(@PathVariable Long gameId,
                       @RequestBody TurnDeployDTO turnDeployDTO) {
        turnService.deployUnits(turnDeployDTO, gameId);
    }

    @PostMapping("/attack")
    @ResponseStatus(HttpStatus.OK)
    public void attack(@PathVariable Long gameId,
                       @RequestBody TurnAttackDTO turnAttackDTO) {
        turnService.attack(turnAttackDTO, gameId);
    }

    @PostMapping("/move")
    @ResponseStatus(HttpStatus.OK)
    public void move(@PathVariable Long gameId,
                     @RequestBody TurnMoveDTO turnMoveDTO) {
        turnService.moveUnits(turnMoveDTO, gameId);
    }
}