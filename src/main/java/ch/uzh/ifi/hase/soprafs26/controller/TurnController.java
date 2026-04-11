package ch.uzh.ifi.hase.soprafs26.controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.service.TurnService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnAttackDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnDeployDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnMoveDTO;

@RestController
public class TurnController {
    private final TurnService turnService;
    TurnController(TurnService turnService) {
        this.turnService = turnService;
    }

    @PutMapping("/turn/deploy/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void deployUnits(@RequestBody TurnDeployDTO turnDeployDTO, @PathVariable Long gameId) {
        turnService.deployUnits(turnDeployDTO, gameId);
    }

    @PutMapping("/turn/attack/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void attack(@RequestBody TurnAttackDTO turnAttackDTO, @PathVariable Long gameId) {
        turnService.attack(turnAttackDTO, gameId);
    }

    @PutMapping("/turn/move/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void moveUnits(@RequestBody TurnMoveDTO turnMoveDTO, @PathVariable Long gameId) {
        turnService.moveUnits(turnMoveDTO, gameId);
    }
}