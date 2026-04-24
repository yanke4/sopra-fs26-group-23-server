package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Map;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Region;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateDTO;

public class GameServiceTest {

    private static final Long GAME_ID = 200L;
    private static final Long PLAYER_A_ID = 10L;
    private static final Long PLAYER_B_ID = 20L;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RegionService regionService;

    private GameService gameService;

    private Game game;
    private Player playerA;
    private Player playerB;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        gameService = new GameService(gameRepository, messagingTemplate, regionService);

        playerA = buildPlayer(PLAYER_A_ID, true);
        playerB = buildPlayer(PLAYER_B_ID, true);

        game = buildGame(GAME_ID, GamePhase.DEPLOY, 0, GameStatus.RUNNING, playerA, playerB);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(regionService.calculateRegionBonus(GAME_ID, playerA)).thenReturn(0);
        when(regionService.calculateRegionBonus(GAME_ID, playerB)).thenReturn(0);
    }

    // --- advancePhase: DEPLOY → ATTACK ---

    @Test
    public void advancePhase_fromDeploy_transitionsToAttack() {
        playerA.setTroopCount(0L); // all troops deployed

        gameService.advancePhase(GAME_ID, PLAYER_A_ID);

        assertEquals(GamePhase.ATTACK, game.getCurrentPhase());
    }

    @Test
    public void advancePhase_fromDeployWithRemainingTroops_throwsBadRequest() {
        playerA.setTroopCount(3L); // still has troops left

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.advancePhase(GAME_ID, PLAYER_A_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(GamePhase.DEPLOY, game.getCurrentPhase()); // phase unchanged
    }

    // --- advancePhase: ATTACK → FORTIFY ---

    @Test
    public void advancePhase_fromAttack_transitionsToFortify() {
        game.setCurrentPhase(GamePhase.ATTACK);

        gameService.advancePhase(GAME_ID, PLAYER_A_ID);

        assertEquals(GamePhase.FORTIFY, game.getCurrentPhase());
    }

    // --- advancePhase: FORTIFY → DEPLOY (next player) ---

    @Test
    public void advancePhase_fromFortify_transitionsToDeployAndAdvancesPlayer() {
        game.setCurrentPhase(GamePhase.FORTIFY);
        game.setCurrentPlayerIndex(0); // playerA is current

        gameService.advancePhase(GAME_ID, PLAYER_A_ID);

        assertEquals(GamePhase.DEPLOY, game.getCurrentPhase());
        assertEquals(1, game.getCurrentPlayerIndex()); // advanced to playerB
    }

    @Test
    public void advancePhase_fromFortify_skipsDeadPlayers() {
        // Add a third dead player between A and B
        Player deadPlayer = buildPlayer(99L, false);
        game.setPlayerOrder(new ArrayList<>(List.of(playerA, deadPlayer, playerB)));
        game.setCurrentPhase(GamePhase.FORTIFY);
        game.setCurrentPlayerIndex(0);

        gameService.advancePhase(GAME_ID, PLAYER_A_ID);

        // Should skip deadPlayer (index 1) and land on playerB (index 2)
        assertEquals(2, game.getCurrentPlayerIndex());
    }

    @Test
    public void advancePhase_fromFortify_resetsMoveDoneThisTurn() {
        game.setCurrentPhase(GamePhase.FORTIFY);
        game.setMoveDoneThisTurn(true);

        gameService.advancePhase(GAME_ID, PLAYER_A_ID);

        assertEquals(false, game.isMoveDoneThisTurn());
    }

    @Test
    public void advancePhase_fromFortify_incrementsTurnNumberOnWrap() {
        game.setCurrentPhase(GamePhase.FORTIFY);
        game.setCurrentPlayerIndex(1); // playerB is last, wraps back to playerA
        game.setTurnNumber(1);
        when(regionService.calculateRegionBonus(GAME_ID, playerA)).thenReturn(0);

        gameService.advancePhase(GAME_ID, PLAYER_B_ID);

        assertEquals(2, game.getTurnNumber());
    }

    @Test
    public void advancePhase_fromFortify_grantsReinforcementsToNextPlayer() {
        game.setCurrentPhase(GamePhase.FORTIFY);
        game.setCurrentPlayerIndex(0);
        when(regionService.calculateRegionBonus(GAME_ID, playerB)).thenReturn(3);

        gameService.advancePhase(GAME_ID, PLAYER_A_ID);

        // base 4 + 3 region bonus = 7
        assertEquals(7L, playerB.getTroopCount());
    }

    // --- advancePhase: error paths ---

    @Test
    public void advancePhase_wrongPlayer_throwsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.advancePhase(GAME_ID, PLAYER_B_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void advancePhase_gameNotFound_throwsNotFound() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.advancePhase(GAME_ID, PLAYER_A_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- advancePhase: broadcasts after success ---

    @Test
    public void advancePhase_success_broadcastsGameState() {
        playerA.setTroopCount(0L);

        gameService.advancePhase(GAME_ID, PLAYER_A_ID);

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/game/" + GAME_ID),
                org.mockito.ArgumentMatchers.any(GameStateDTO.class));
    }

    // --- getGameState ---

    @Test
    public void getGameState_gameNotFound_throwsNotFound() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getGameState(GAME_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getGameState_validGame_returnsCorrectGameId() {
        GameStateDTO dto = gameService.getGameState(GAME_ID);

        assertEquals(GAME_ID, dto.getGameId());
    }

    @Test
    public void getGameState_validGame_returnsCorrectStatus() {
        GameStateDTO dto = gameService.getGameState(GAME_ID);

        assertEquals(GameStatus.RUNNING, dto.getStatus());
    }

    @Test
    public void getGameState_validGame_returnsCorrectCurrentPlayerId() {
        GameStateDTO dto = gameService.getGameState(GAME_ID);

        assertEquals(PLAYER_A_ID, dto.getCurrentPlayerId());
    }

    // --- helpers ---

    private Player buildPlayer(Long id, boolean alive) {
        Player p = new Player();
        p.setPlayerId(id);
        p.setAlive(alive);
        p.setTroopCount(0L);
        User u = new User();
        u.setId(id);
        u.setUsername("user-" + id);
        p.setUser(u);
        return p;
    }

    private Game buildGame(Long id, GamePhase phase, int currentIndex,
                           GameStatus status, Player... players) {
        Game g = new Game();
        g.setId(id);
        g.setCurrentPhase(phase);
        g.setCurrentPlayerIndex(currentIndex);
        g.setStatus(status);
        g.setTurnNumber(1);
        g.setPlayerOrder(new ArrayList<>(List.of(players)));

        // Minimal map so convertToGameStateDTO doesn't NPE
        Region region = new Region();
        region.setFields(new ArrayList<>());
        Map map = new Map();
        map.setRegions(List.of(region));
        g.setMap(map);

        return g;
    }
}