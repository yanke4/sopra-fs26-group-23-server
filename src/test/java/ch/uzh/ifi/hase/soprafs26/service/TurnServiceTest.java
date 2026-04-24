package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;



import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnAttackDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnDeployDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnMoveDTO;

public class TurnServiceTest {

    protected static final Long GAME_ID = 100L;
    protected static final Long ACTIVE_PLAYER_ID = 1L;
    protected static final Long OTHER_PLAYER_ID = 2L;

    @Mock
    protected FieldService fieldService;

    @Mock
    protected GameService gameService;

    @Mock
    protected GameRepository gameRepository;

    @Mock
    protected RegionService regionService;

    protected TurnService turnService;

    protected Game game;
    protected Player activePlayer;
    protected Player otherPlayer;

    protected Field fieldA;
    protected Field fieldB;
    protected Field fieldC;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        turnService = new TurnService(fieldService, gameService, gameRepository, regionService);

        activePlayer = createPlayer(ACTIVE_PLAYER_ID, 10L, true);
        otherPlayer = createPlayer(OTHER_PLAYER_ID, 10L, true);

        game = createGame(GAME_ID, GamePhase.DEPLOY, 0, false, GameStatus.RUNNING, activePlayer, otherPlayer);

        fieldA = createField(11L, "A", activePlayer, 5L);
        fieldB = createField(12L, "B", activePlayer, 3L);
        fieldC = createField(13L, "C", otherPlayer, 2L);

        makeFieldNeighbours(fieldA, fieldB);
        makeFieldNeighbours(fieldB, fieldC);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
    }


    // Helper methods to create entities
    protected Player createPlayer(Long playerId, Long troopCount, boolean alive) {
        Player player = new Player();
        player.setPlayerId(playerId);
        player.setTroopCount(troopCount);
        player.setAlive(alive);

        User user = new User();
        user.setId(playerId);
        user.setUsername("user-" + playerId);
        player.setUser(user);

        return player;
    }
    protected Game createGame(Long gameId, GamePhase phase, int currentPlayerIndex, boolean moveDoneThisTurn,
            GameStatus status, Player... players) {
        Game createdGame = new Game();
        createdGame.setId(gameId);
        createdGame.setCurrentPhase(phase);
        createdGame.setCurrentPlayerIndex(currentPlayerIndex);
        createdGame.setMoveDoneThisTurn(moveDoneThisTurn);
        createdGame.setStatus(status);
        createdGame.setTurnNumber(1);
        createdGame.setPlayerOrder(List.of(players));
        return createdGame;
    }
    protected Field createField(Long fieldId, String name, Player owner, Long troops) {
        Field field = new Field();
        field.setFieldID(fieldId);
        field.setName(name);
        field.setOwner(owner);
        field.setTroops(troops);
        field.setNeighbours(new ArrayList<>());
        return field;
    }

    // Helper methods for field entity
    protected void makeFieldNeighbours(Field first,
            Field second) {
        first.getNeighbours().add(second);
        second.getNeighbours().add(first);
    }

    
    // Helper methods to mock deployment
    protected TurnDeployDTO buildDeployDto(Long playerId, List<TurnDeployDTO.Deployment> deployments) {
        TurnDeployDTO dto = new TurnDeployDTO();
        dto.setPlayerId(playerId);
        dto.setDeployments(deployments);
        return dto;
    }
    protected TurnDeployDTO.Deployment buildDeployment(String fieldName, Long troops) {
        TurnDeployDTO.Deployment deployment = new TurnDeployDTO.Deployment();
        deployment.setFieldName(fieldName);
        deployment.setTroops(troops);
        return deployment;
    }

    // Helper methods to mock moves
    protected TurnMoveDTO buildMoveDto(Long playerId, List<TurnMoveDTO.Move> moves) {
        TurnMoveDTO dto = new TurnMoveDTO();
        dto.setPlayerId(playerId);
        dto.setMoves(moves);
        return dto;
    }
    protected TurnMoveDTO.Move buildMove(String fromField, String toField, Long troops) {
        TurnMoveDTO.Move move = new TurnMoveDTO.Move();
        move.setFromField(fromField);
        move.setToField(toField);
        move.setTroops(troops);
        return move;
    }

    // Helper methods to mock attacks
    protected TurnAttackDTO buildAttackDto(Long playerId, List<TurnAttackDTO.Attack> attacks) {
        TurnAttackDTO dto = new TurnAttackDTO();
        dto.setPlayerId(playerId);
        dto.setAttacks(attacks);
        return dto;
    }
    protected TurnAttackDTO.Attack buildAttack(String attackingField, String defendingField, Long troops) {
        TurnAttackDTO.Attack attack = new TurnAttackDTO.Attack();
        attack.setAttackingField(attackingField);
        attack.setDefendingField(defendingField);
        attack.setTroops(troops);
        return attack;
    }


    // ------Testing deployUnits-------
    @Test
    public void deployUnits_validOwnedFieldsAndEnoughTroops_deploysAndBroadcasts() {
        game.setCurrentPhase(GamePhase.DEPLOY); // set phase to deploy

        // Mock fieldService to return fieldA when requested
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);

        // Build deployment DTO-> deploy 5 troops to A (total 5, player has 10)
        TurnDeployDTO dto = buildDeployDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildDeployment("A", 5L)
            )
        );
        long initialTroops = activePlayer.getTroopCount();

        // Call method under test
        turnService.deployUnits(dto, GAME_ID);

        assertEquals(initialTroops - 5L, activePlayer.getTroopCount()); // Assert Player should have 5 troops
        verify(fieldService).addUnits("A", 5L, GAME_ID); // Verify field units were added
        verify(gameService).broadcastGameUpdate(GAME_ID); // Verify broadcast was called
    }
    @Test
    public void deployUnits_wrongPlayer_throwsForbidden() {
        game.setCurrentPhase(GamePhase.DEPLOY); // set phase to deploy

        // Build deployment DTO with wrong player id
        TurnDeployDTO dto = buildDeployDto(
            OTHER_PLAYER_ID,
            List.of(
                buildDeployment("A", 5L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> turnService.deployUnits(dto, GAME_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(fieldService, never()).addUnits(anyString(), anyLong(), anyLong());
        verify(gameService, never()).broadcastGameUpdate(GAME_ID);
    }
    @Test
    public void deployUnits_wrongPhase_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.ATTACK); // wrong phase for deploy

        // Build deployment DTO with correct player id
        TurnDeployDTO dto = buildDeployDto(
            ACTIVE_PLAYER_ID,
            List.of(buildDeployment("A", 5L)
        ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.deployUnits(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(fieldService, never()).addUnits(anyString(), anyLong(), anyLong()); // Verify no units were added
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }
    @Test
    public void deployUnits_totalTroopsExceedAvailable_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.DEPLOY); // set phase to deploy

        // Build deployment DTO with more troops than available (11 > 10)
        TurnDeployDTO dto = buildDeployDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildDeployment("A", 11L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.deployUnits(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode()); 
        verify(fieldService, never()).addUnits(anyString(), anyLong(), anyLong()); // Verify no units were added
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }
    @Test
    public void deployUnits_targetFieldNotOwned_throwsForbidden() {
        game.setCurrentPhase(GamePhase.DEPLOY); // set phase to deploy

        // Mock fieldService to return fieldC which is not owned by activePlayer
        when(fieldService.getFieldByName("C", GAME_ID)).thenReturn(fieldC);

        // Build deployment DTO targeting field C
        TurnDeployDTO dto = buildDeployDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildDeployment("C", 5L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.deployUnits(dto, GAME_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(fieldService, never()).addUnits("C", 5L, GAME_ID); // Verify no units were added to field C
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }


    // ------Testing attack------
    @Test
    public void attack_validRequest_flushesAndBroadcasts() {
        game.setCurrentPhase(GamePhase.ATTACK); // set phase to attack

        // Ensure defending field is enemy and adjacent
        fieldB.setOwner(otherPlayer);
        fieldA.setTroops(5L);
        fieldB.setTroops(2L);

        // Mock fieldService for attack fields and win-condition checks
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);
        when(fieldService.getFieldByName("B", GAME_ID)).thenReturn(fieldB);
        when(fieldService.countTerritoriesOwnedByPlayer(GAME_ID, activePlayer)).thenReturn(1);
        when(fieldService.countTerritoriesOwnedByPlayer(GAME_ID, otherPlayer)).thenReturn(1);

        // Build attack DTO from A to B
        TurnAttackDTO dto = buildAttackDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildAttack("A", "B", 3L)
            ));

        // Call method under test
        turnService.attack(dto, GAME_ID);

        assertEquals(2L, fieldA.getTroops()); // 2 left on attacking field
        // Due to dice roll, the defending field will end up with 1-3 troops (depends on who wins/loses)
        assertTrue(fieldB.getTroops() >= 1 && fieldB.getTroops() <= 3);

        verify(gameRepository).flush(); // Verify game state was flushed
        verify(gameService).broadcastGameUpdate(GAME_ID); // Verify broadcast was called
    }
    @Test
    public void attack_wrongPlayer_throwsForbidden() {
        game.setCurrentPhase(GamePhase.ATTACK); // set phase to attack

        // Build attack DTO with wrong player id
        TurnAttackDTO dto = buildAttackDto(
            OTHER_PLAYER_ID,
            List.of(
                buildAttack("A", "C", 2L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.attack(dto, GAME_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(fieldService, never()).getFieldByName(anyString(), anyLong()); // Verify no field lookup happened
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }
    @Test
    public void attack_wrongPhase_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.FORTIFY); // wrong phase for attack

        // Build attack DTO with correct player id
        TurnAttackDTO dto = buildAttackDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildAttack("A", "C", 2L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.attack(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(fieldService, never()).getFieldByName(anyString(), anyLong()); // Verify no field lookup happened
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }
    @Test
    public void attack_attackingOwnTerritory_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.ATTACK); // set phase to attack

        // Mock fieldService to return two fields owned by activePlayer
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);
        when(fieldService.getFieldByName("B", GAME_ID)).thenReturn(fieldB);

        // Build attack DTO targeting own territory B
        TurnAttackDTO dto = buildAttackDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildAttack("A", "B", 2L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.attack(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }
    @Test
    public void attack_nonAdjacentFields_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.ATTACK); // set phase to attack

        // Mock fieldService to return fieldA and fieldC which are not adjacent
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);
        when(fieldService.getFieldByName("C", GAME_ID)).thenReturn(fieldC);

        // Build attack DTO from A to C
        TurnAttackDTO dto = buildAttackDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildAttack("A", "C", 2L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.attack(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }
    @Test
    public void attack_attackingFieldHasLessThanTwoTroops_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.ATTACK); // set phase to attack

        // Set troops below minimum and ensure defending field is enemy and adjacent
        fieldA.setTroops(1L);
        fieldB.setOwner(otherPlayer);
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);
        when(fieldService.getFieldByName("B", GAME_ID)).thenReturn(fieldB);

        // Build attack DTO from A to B
        TurnAttackDTO dto = buildAttackDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildAttack("A", "B", 1L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.attack(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }
    @Test
    public void attack_troopsLeftBehindLessThanOne_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.ATTACK); // set phase to attack

        // Ensure defending field is enemy and adjacent
        fieldB.setOwner(otherPlayer);
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);
        when(fieldService.getFieldByName("B", GAME_ID)).thenReturn(fieldB);

        // Build attack DTO with troops used equal to attacking troops
        TurnAttackDTO dto = buildAttackDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildAttack("A", "B", 5L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.attack(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }


    // ------Testing moveUnits------
    @Test
    public void moveUnits_validPath_movesTroopsAndBroadcasts() {
        game.setCurrentPhase(GamePhase.FORTIFY); // set phase to fortify
        game.setMoveDoneThisTurn(false);

        // Mock fieldService to return fieldA and fieldB when requested
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);
        when(fieldService.getFieldByName("B", GAME_ID)).thenReturn(fieldB);

        // Build move DTO-> move 2 troops from A to B
        TurnMoveDTO dto = buildMoveDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildMove("A", "B", 2L)
            ));

        // Call method under test
        turnService.moveUnits(dto, GAME_ID);

        assertEquals(true, game.isMoveDoneThisTurn()); // Assert move was marked as done
        verify(fieldService).removeUnits("A", 2L, GAME_ID); // Verify units were removed from source field
        verify(fieldService).addUnits("B", 2L, GAME_ID); // Verify units were added to target field
        verify(gameRepository).save(game); // Verify game state was saved
        verify(gameRepository).flush(); // Verify game state was flushed
        verify(gameService).broadcastGameUpdate(GAME_ID); // Verify broadcast was called
    }

    @Test
    public void moveUnits_wrongPlayer_throwsForbidden() {
        game.setCurrentPhase(GamePhase.FORTIFY); // set phase to fortify

        // Build move DTO with wrong player id
        TurnMoveDTO dto = buildMoveDto(
            OTHER_PLAYER_ID,
            List.of(
                buildMove("A", "B", 2L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.moveUnits(dto, GAME_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(fieldService, never()).removeUnits(anyString(), anyLong(), anyLong()); // Verify no units were removed
        verify(fieldService, never()).addUnits(anyString(), anyLong(), anyLong()); // Verify no units were added
        verify(gameRepository, never()).save(game); // Verify game state was not saved
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }

    @Test
    public void moveUnits_wrongPhase_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.ATTACK); // wrong phase for move

        // Build move DTO with correct player id
        TurnMoveDTO dto = buildMoveDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildMove("A", "B", 2L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.moveUnits(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(fieldService, never()).removeUnits(anyString(), anyLong(), anyLong()); // Verify no units were removed
        verify(fieldService, never()).addUnits(anyString(), anyLong(), anyLong()); // Verify no units were added
        verify(gameRepository, never()).save(game); // Verify game state was not saved
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }

    @Test
    public void moveUnits_moveAlreadyDoneThisTurn_throwsForbidden() {
        game.setCurrentPhase(GamePhase.FORTIFY); // set phase to fortify
        game.setMoveDoneThisTurn(true); // move has already been done this turn

        // Build move DTO with correct player id
        TurnMoveDTO dto = buildMoveDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildMove("A", "B", 2L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.moveUnits(dto, GAME_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(fieldService, never()).removeUnits(anyString(), anyLong(), anyLong()); // Verify no units were removed
        verify(fieldService, never()).addUnits(anyString(), anyLong(), anyLong()); // Verify no units were added
        verify(gameRepository, never()).save(game); // Verify game state was not saved
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }

    @Test
    public void moveUnits_noConnectedOwnedPath_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.FORTIFY); // set phase to fortify
        game.setMoveDoneThisTurn(false);

        // Ensure target field is owned but not connected through owned fields
        fieldC.setOwner(activePlayer);
        fieldA.setNeighbours(new ArrayList<>());
        fieldC.setNeighbours(new ArrayList<>());

        // Mock fieldService to return source and target fields
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);
        when(fieldService.getFieldByName("C", GAME_ID)).thenReturn(fieldC);

        // Build move DTO: move from A to C
        TurnMoveDTO dto = buildMoveDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildMove("A", "C", 2L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.moveUnits(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(fieldService, never()).removeUnits(anyString(), anyLong(), anyLong()); // Verify no units were removed
        verify(fieldService, never()).addUnits(anyString(), anyLong(), anyLong()); // Verify no units were added
        verify(gameRepository, never()).save(game); // Verify game state was not saved
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }

    @Test
    public void moveUnits_notEnoughTroopsToLeaveOneBehind_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.FORTIFY); // set phase to fortify
        game.setMoveDoneThisTurn(false);

        // Mock fieldService to return fieldA and fieldB when requested
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);
        when(fieldService.getFieldByName("B", GAME_ID)).thenReturn(fieldB);

        // Build move DTO: try moving all troops from A (must leave at least 1)
        TurnMoveDTO dto = buildMoveDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildMove("A", "B", 5L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.moveUnits(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(fieldService, never()).removeUnits(anyString(), anyLong(), anyLong()); // Verify no units were removed
        verify(fieldService, never()).addUnits(anyString(), anyLong(), anyLong()); // Verify no units were added
        verify(gameRepository, never()).save(game); // Verify game state was not saved
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }

    @Test
    public void moveUnits_nonPositiveTroops_throwsBadRequest() {
        game.setCurrentPhase(GamePhase.FORTIFY); // set phase to fortify
        game.setMoveDoneThisTurn(false);

        // Mock fieldService to return fieldA and fieldB when requested
        when(fieldService.getFieldByName("A", GAME_ID)).thenReturn(fieldA);
        when(fieldService.getFieldByName("B", GAME_ID)).thenReturn(fieldB);

        // Build move DTO: move 0 troops (invalid)
        TurnMoveDTO dto = buildMoveDto(
            ACTIVE_PLAYER_ID,
            List.of(
                buildMove("A", "B", 0L)
            ));

        // Call method under test
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> turnService.moveUnits(dto, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(fieldService, never()).removeUnits(anyString(), anyLong(), anyLong()); // Verify no units were removed
        verify(fieldService, never()).addUnits(anyString(), anyLong(), anyLong()); // Verify no units were added
        verify(gameRepository, never()).save(game); // Verify game state was not saved
        verify(gameRepository, never()).flush(); // Verify game state was not flushed
        verify(gameService, never()).broadcastGameUpdate(GAME_ID); // Verify no broadcast was called
    }
}
