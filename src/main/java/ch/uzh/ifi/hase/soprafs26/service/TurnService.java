package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
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
    private final GameRepository gameRepository;
    private final RegionService regionService;

    public TurnService(FieldService fieldService, GameService gameService, GameRepository gameRepository, RegionService regionService) {
        this.fieldService = fieldService;
        this.gameService = gameService;
        this.gameRepository = gameRepository;
        this.regionService = regionService;
    }

    public void deployUnits(TurnDeployDTO turnDeployDTO, Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));
        Player activePlayer = game.getCurrentPlayer();
        if (activePlayer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active player found for the game.");
        }
        if (!activePlayer.getPlayerId().equals(turnDeployDTO.getPlayerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not player " + turnDeployDTO.getPlayerId() + "'s turn.");
        }
        if (game.getCurrentPhase() != GamePhase.DEPLOY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deploy outside of DEPLOY phase.");
        }

        for (Deployment deployment : turnDeployDTO.getDeployments()) {
            String fieldName = deployment.getFieldName();
            Long troops = deployment.getTroops();

            Field field = fieldService.getFieldByName(fieldName, gameId);
            if (field.getOwner() == null || !field.getOwner().getPlayerId().equals(activePlayer.getPlayerId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player " + activePlayer.getPlayerId() + " does not own the territory: " + fieldName);
            }
            fieldService.addUnits(fieldName, troops, gameId);
        }
        //Actualize Game state and send update to clients via WebSocket
        gameService.broadcastGameUpdate(gameId);


    }

    public void attack(TurnAttackDTO turnAttackDTO, Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));
        Player activePlayer = game.getCurrentPlayer();
        if (activePlayer == null || !activePlayer.getPlayerId().equals(turnAttackDTO.getPlayerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not player " + turnAttackDTO.getPlayerId() + "'s turn.");
        }
        if (game.getCurrentPhase() != GamePhase.ATTACK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot attack outside of ATTACK phase.");
        }

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

        gameRepository.flush();
        gameService.broadcastGameUpdate(gameId);
    }

    public void moveUnits(TurnMoveDTO turnMoveDTO, Long gameId) {

        Game game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));
        Player activePlayer = game.getCurrentPlayer();
        if (activePlayer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active player found for the game.");
        }
        if (!activePlayer.getPlayerId().equals(turnMoveDTO.getPlayerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not player " + turnMoveDTO.getPlayerId() + "'s turn.");
        }
        if (game.getCurrentPhase() != GamePhase.FORTIFY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move troops outside of FORTIFY phase.");
        }

        if (game.isMoveDoneThisTurn()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player has already moved troops this turn.");
        }

        for (Move move : turnMoveDTO.getMoves()) {
            String fromFieldName = move.getFromField();
            String toFieldName = move.getToField();
            Long troops = move.getTroops();
            Field fromField = fieldService.getFieldByName (fromFieldName, gameId);
            Field toField = fieldService.getFieldByName(toFieldName, gameId);
            if (fromField.getOwner() == null || !fromField.getOwner().getPlayerId().equals(activePlayer.getPlayerId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player " + activePlayer.getPlayerId() + " does not own the territory: " + fromFieldName);
            }

            if (toField.getOwner() == null || !toField.getOwner().getPlayerId().equals(activePlayer.getPlayerId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player " + activePlayer.getPlayerId() + " does not own the territory: " + toFieldName);
            }

            if (!isConnectedThroughOwnedFields(fromField, toField, activePlayer)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No connected path of your territories from " + fromFieldName + " to " + toFieldName);
            }

            if (troops == null || troops <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Troops to move must be positive.");
            }
            if (fromField.getTroops() == null || fromField.getTroops() - troops < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must leave at least 1 troop on " + fromFieldName);
            }
            fieldService.removeUnits(fromFieldName, troops, gameId);
            fieldService.addUnits(toFieldName, troops, gameId);
        }
        game.setMoveDoneThisTurn(true);
        gameRepository.save(game);
        gameRepository.flush();
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

    // helper method to calculate the number of troops a player receives
    private Long calculateReinforcements(Long gameId, Player player){
        int territoryCount = fieldService.countTerritoriesOwnedByPlayer(gameId, player);
        int reinforcementsFromTerritories = territoryCount * 5; 
        int reinforcementsFromRegions = regionService.calculateRegionBonus(gameId, player);
        return (long) reinforcementsFromTerritories +  (long)reinforcementsFromRegions;
    }


    private boolean isConnectedThroughOwnedFields(Field start, Field end, Player player) {
    if (start.getFieldID().equals(end.getFieldID())) return true;

    java.util.Set<Long> visited = new java.util.HashSet<>();
    java.util.Queue<Field> queue = new java.util.LinkedList<>();
    queue.add(start);
    visited.add(start.getFieldID());

    while (!queue.isEmpty()) {
        Field current = queue.poll();
        List<Field> neighbours = current.getNeighbours();
        if (neighbours == null) continue;

        for (Field neighbour : neighbours) {
            if (visited.contains(neighbour.getFieldID())) continue;
            if (neighbour.getOwner() == null 
                    || !neighbour.getOwner().getPlayerId().equals(player.getPlayerId())) continue;

            if (neighbour.getFieldID().equals(end.getFieldID())) return true;

            visited.add(neighbour.getFieldID());
            queue.add(neighbour);
        }
    }
    return false;
}
}
