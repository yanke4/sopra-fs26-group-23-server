package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnAttackDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnAttackDTO.Attack;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnDeployDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnDeployDTO.Deployment;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnMoveDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnMoveDTO.Move;


@Service
public class TurnService {

    private final FieldService fieldService;
    private final GameService gameService;

    public TurnService(FieldService fieldService, GameService gameService) {
        this.fieldService = fieldService;
        this.gameService = gameService;
    }

    public void deployUnits(TurnDeployDTO turnDeployDTO, Long gameId) {
        for (Deployment deployment : turnDeployDTO.getDeployments()) {
            String fieldName = deployment.getFieldName();
            Long troops = deployment.getTroops();
            fieldService.addUnits(fieldName, troops, gameId);
        }
        //Actualize Game state and send update to clients via WebSocket
        gameService.broadcastGameUpdate(gameId);


    }

    public void attack(TurnAttackDTO turnAttackDTO, Long gameId) {
        Long requestingPlayerId = turnAttackDTO.getPlayerId();
        
        for (Attack attack: turnAttackDTO.getAttacks()) {
            Field attackingField = fieldService.getFieldByName(attack.getAttackingField(), gameId);
            Field defendingField = fieldService.getFieldByName(attack.getDefendingField(), gameId);

            if (attackingField.getOwner() == null || !attackingField.getOwner().getPlayerId().equals(requestingPlayerId)) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,"Player " + requestingPlayerId + " does not own the attacking territory: " + attack.getAttackingField());
        }

            if (defendingField.getOwner() != null && defendingField.getOwner().getPlayerId().equals(requestingPlayerId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cannot attack your own territory: " + attack.getDefendingField());
        }

        boolean isAdjacent = attackingField.getNeighbours() != null && attackingField.getNeighbours().stream()
                .anyMatch(n -> n.getName().equals(defendingField.getName()));

        if (!isAdjacent) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, attack.getDefendingField() + " is not adjacent to " + attack.getAttackingField());
        }

        if (attackingField.getTroops() == null || attackingField.getTroops() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Need at least 2 troops on " + attack.getAttackingField() + " to attack.");
        }
            Long attackingTroops = attack.getTroops();
            Long defendingTroops = defendingField.getTroops();
            Long attackingLosses = 0L;
            Long defendingLosses = 0L;
            //attack logic: 
        while (defendingTroops > 0 && attackingTroops > 1) {
            List<Integer> attackerRolls = rollDices(attackingTroops, 3);
            List<Integer> defenderRolls = rollDices(defendingTroops, 2);

            int comparisons = Math.min(attackerRolls.size(), defenderRolls.size());
            for (int i = 0; i < comparisons; i++) {
                if (attackerRolls.get(i) > defenderRolls.get(i)) {
                    defendingLosses += 1L;
                    defendingTroops -= 1L;

                } else {
                    attackingLosses += 1L;
                    attackingTroops -= 1L;
                }
            }
        }

        if (defendingTroops == 0) {
            attackingField.setTroops(1L);
            defendingField.setTroops(attackingTroops-1L);
            defendingField.setOwner(attackingField.getOwner());
    
        } else {
            attackingField.setTroops(1L);
            defendingField.setTroops(defendingTroops);
        }
        }
        //Actualize Game state and send update to clients via WebSocket
        gameService.broadcastGameUpdate(gameId);
    }

    public void moveUnits(TurnMoveDTO turnMoveDTO, Long gameId) {
        for (Move move : turnMoveDTO.getMoves()) {
            String fromFieldName = move.getFromField();
            String toFieldName = move.getToField();
            Long troops = move.getTroops();
            fieldService.removeUnits(fromFieldName, troops, gameId);
            fieldService.addUnits(toFieldName, troops, gameId);
        }
        //Actualize Game state and send update to clients via WebSocket
        gameService.broadcastGameUpdate(gameId);
    }

    // helper method to roll dices for attack
    private List<Integer> rollDices(Long troopCount, int maxDice) {
        List<Integer> rolls = new ArrayList<>();
        int numDice = Math.min(troopCount.intValue(), maxDice);
        for (int i = 0; i < numDice; i++) {
            rolls.add(ThreadLocalRandom.current().nextInt(1, 7));
        }
        rolls.sort(Collections.reverseOrder());
        return rolls;
    }

}
