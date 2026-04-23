package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Region;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

@Service
@Transactional
public class PlayerService {

    private final GameRepository gameRepository;
    private final GameService gameService;

    public PlayerService(GameRepository gameRepository, GameService gameService) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
    }

    public void surrender(Long gameId, Long playerId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));

        Player surrenderingPlayer = game.getPlayerOrder().stream()
            .filter(p -> p.getPlayerId().equals(playerId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Player not found in game."));

        if (!surrenderingPlayer.isAlive()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Player has already surrendered.");
        }

        for (Region region : game.getMap().getRegions()) {
            for (Field field : region.getFields()) {
                if (field.getOwner() != null
                        && field.getOwner().getPlayerId().equals(playerId)) {
                    field.setOwner(null);
                }
            }
        }

        surrenderingPlayer.setAlive(false);
        surrenderingPlayer.setTroopCount(0L);

        boolean wasTheirTurn = game.getCurrentPlayer() != null
            && game.getCurrentPlayer().getPlayerId().equals(playerId);

        if (wasTheirTurn) {
            List<Player> players = game.getPlayerOrder();
            int nextIndex = game.getCurrentPlayerIndex();
            int startIndex = nextIndex;
            do {
                nextIndex = (nextIndex + 1) % players.size();
            } while (!players.get(nextIndex).isAlive() && nextIndex != startIndex);

            game.setCurrentPlayerIndex(nextIndex);
            game.setCurrentPhase(GamePhase.DEPLOY);
            game.setMoveDoneThisTurn(false);
        }


        gameRepository.save(game);
        gameRepository.flush();
        
        long aliveCount = game.getPlayerOrder().stream()
        .filter(Player::isAlive)
        .count();

        if (aliveCount <= 1) {
            game.setStatus(GameStatus.FINISHED);
            gameRepository.save(game);
            gameRepository.flush();
        }

        gameService.broadcastGameState(game);
    }
}