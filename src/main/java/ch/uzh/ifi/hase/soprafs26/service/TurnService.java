package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnAttackDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnAttackDTO.Attack;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnDeployDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnDeployDTO.Deployment;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnMoveDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnMoveDTO.Move;
import ch.uzh.ifi.hase.soprafs26.service.FieldService;

import ch.uzh.ifi.hase.soprafs26.entity.Field;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

@Service
public class TurnService {

    private final FieldService fieldService;
    public TurnService(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    public void deployUnits(TurnDeployDTO turnDeployDTO, Long gameId) {
        for (Deployment deployment : turnDeployDTO.getDeployments()) {
            String fieldName = deployment.getFieldName();
            Long troops = deployment.getTroops();
            fieldService.addUnits(fieldName, troops, gameId);
        }
        //Actualize Game state and send update to clients via WebSocket (not implemented yet)

    }

    public void attack(TurnAttackDTO turnAttackDTO, Long gameId) {
        for (Attack attack: turnAttackDTO.getAttacks()) {
            Field attackingField = fieldService.getFieldByName(attack.getAttackingField(), gameId);
            Field defendingField = fieldService.getFieldByName(attack.getDefendingField(), gameId);
            Long attackingTroops = attack.getTroops();
            Long defendingTroops = defendingField.getTroops();
            //attack logic: 
            List<Integer> attackerRolls = rollDices(attackingTroops, 3);
            List<Integer> defenderRolls = rollDices(defendingTroops, 2);

            int comparisons = Math.min(attackerRolls.size(), defenderRolls.size());
            for (int i = 0; i < comparisons; i++) {
                if (attackerRolls.get(i) > defenderRolls.get(i)) {
                    fieldService.removeUnits(defendingField.getName(), 1L, gameId);
                } else {
                    fieldService.removeUnits(attackingField.getName(), 1L, gameId);
                }
            }
        }
        //Actualize Game state and send update to clients via WebSocket (not implemented yet)
    }

    public void moveUnits(TurnMoveDTO turnMoveDTO, Long gameId) {
        for (Move move : turnMoveDTO.getMoves()) {
            String fromFieldName = move.getFromField();
            String toFieldName = move.getToField();
            Long troops = move.getTroops();
            fieldService.removeUnits(fromFieldName, troops, gameId);
            fieldService.addUnits(toFieldName, troops, gameId);
        }
        //Actualize Game state and send update to clients via WebSocket (not implemented yet)
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
