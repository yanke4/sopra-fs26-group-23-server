package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Map;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Region;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

public class PlayerServiceTest {

    private static final Long GAME_ID = 100L;
    private static final Long PLAYER_A_ID = 1L;
    private static final Long PLAYER_B_ID = 2L;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameService gameService;

    @Mock
    private UserService userService;

    private PlayerService playerService;

    private Game game;
    private Player playerA;
    private Player playerB;
    private Field fieldOwnedByA;
    private Field fieldOwnedByB;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        playerService = new PlayerService(gameRepository, gameService, userService);

        playerA = buildPlayer(PLAYER_A_ID, true);
        playerB = buildPlayer(PLAYER_B_ID, true);

        fieldOwnedByA = buildField(1L, "FieldA", playerA);
        fieldOwnedByB = buildField(2L, "FieldB", playerB);

        Region region = new Region();
        region.setFields(List.of(fieldOwnedByA, fieldOwnedByB));

        Map map = new Map();
        map.setRegions(List.of(region));

        game = new Game();
        game.setId(GAME_ID);
        game.setStatus(GameStatus.RUNNING);
        game.setCurrentPhase(GamePhase.ATTACK);
        game.setCurrentPlayerIndex(1); // playerB is current
        game.setPlayerOrder(new ArrayList<>(List.of(playerA, playerB)));
        game.setMap(map);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
    }

    // --- surrender: basic success ---

    @Test
    public void surrender_validPlayer_setsAliveToFalse() {
        playerService.surrender(GAME_ID, PLAYER_A_ID);

        assertFalse(playerA.isAlive());
    }

    @Test
    public void surrender_validPlayer_troopCountSetToZero() {
        playerA.setTroopCount(5L);

        playerService.surrender(GAME_ID, PLAYER_A_ID);

        assertEquals(0L, playerA.getTroopCount());
    }

    @Test
    public void surrender_validPlayer_territoriesAreNeutralized() {
        playerService.surrender(GAME_ID, PLAYER_A_ID);

        assertNull(fieldOwnedByA.getOwner());
        // playerB's field must remain untouched
        assertEquals(playerB, fieldOwnedByB.getOwner());
    }

    // --- surrender: turn-advance when it is the surrendering player's turn ---

    @Test
    public void surrender_wasTheirTurn_advancesToNextAlivePlayer() {
        // Make playerB (index 1) the current player and surrendering player
        game.setCurrentPlayerIndex(1);

        playerService.surrender(GAME_ID, PLAYER_B_ID);

        // After playerB surrenders, turn should wrap back to playerA (index 0)
        assertEquals(0, game.getCurrentPlayerIndex());
        assertEquals(GamePhase.DEPLOY, game.getCurrentPhase());
        assertFalse(game.isMoveDoneThisTurn());
    }

    @Test
    public void surrender_wasNotTheirTurn_doesNotChangeTurnIndex() {
        // playerB (index 1) is current; playerA surrenders out of turn
        game.setCurrentPlayerIndex(1);

        playerService.surrender(GAME_ID, PLAYER_A_ID);

        // Current player index must stay at 1
        assertEquals(1, game.getCurrentPlayerIndex());
    }

    // --- surrender: win-condition ---

    @Test
    public void surrender_lastRemainingOpponentSurrenders_gameSetToFinished() {
        // playerA is already eliminated; playerB surrenders last
        playerA.setAlive(false);

        playerService.surrender(GAME_ID, PLAYER_B_ID);

        assertEquals(GameStatus.FINISHED, game.getStatus());
        verify(userService).updatePlayerStats(game);
    }

    @Test
    public void surrender_gameNotFinished_doesNotUpdateStats() {
        playerService.surrender(GAME_ID, PLAYER_A_ID);

        verify(userService, never()).updatePlayerStats(game);
    }


    @Test
    public void surrender_alreadySurrendered_throwsBadRequest() {
        playerA.setAlive(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> playerService.surrender(GAME_ID, PLAYER_A_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void surrender_playerNotInGame_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> playerService.surrender(GAME_ID, 999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void surrender_gameNotFound_throwsNotFound() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> playerService.surrender(GAME_ID, PLAYER_A_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void surrender_broadcastsGameState() {
        playerService.surrender(GAME_ID, PLAYER_A_ID);

        verify(gameService).broadcastGameState(game);
    }

    // --- helpers ---

    private Player buildPlayer(Long id, boolean alive) {
        Player p = new Player();
        p.setPlayerId(id);
        p.setAlive(alive);
        p.setTroopCount(3L);
        User u = new User();
        u.setId(id);
        u.setUsername("user-" + id);
        p.setUser(u);
        return p;
    }

    private Field buildField(Long id, String name, Player owner) {
        Field f = new Field();
        f.setFieldID(id);
        f.setName(name);
        f.setOwner(owner);
        f.setTroops(2L);
        return f;
    }
}