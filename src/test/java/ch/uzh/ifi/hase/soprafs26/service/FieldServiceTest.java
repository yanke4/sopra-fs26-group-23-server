package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Map;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Region;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

public class FieldServiceTest {

    private static final Long GAME_ID = 300L;

    @Mock
    private GameRepository gameRepository;

    private FieldService fieldService;

    private Game game;
    private Player playerA;
    private Field fieldA;
    private Field fieldB;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        fieldService = new FieldService(gameRepository);

        playerA = new Player();
        playerA.setPlayerId(1L);

        fieldA = buildField(1L, "Alpha", playerA, 5L);
        fieldB = buildField(2L, "Beta", null, 1L);

        Region region = new Region();
        region.setFields(new ArrayList<>(List.of(fieldA, fieldB)));

        Map map = new Map();
        map.setRegions(List.of(region));

        game = new Game();
        game.setId(GAME_ID);
        game.setMap(map);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
    }

    // --- getFieldByName ---

    @Test
    public void getFieldByName_existingField_returnsField() {
        Field result = fieldService.getFieldByName("Alpha", GAME_ID);

        assertEquals("Alpha", result.getName());
        assertEquals(playerA, result.getOwner());
    }

    @Test
    public void getFieldByName_unknownField_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fieldService.getFieldByName("Nonexistent", GAME_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getFieldByName_gameNotFound_throwsNotFound() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fieldService.getFieldByName("Alpha", GAME_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- addUnits ---

    @Test
    public void addUnits_validAmount_increasesTroopCount() {
        fieldService.addUnits("Alpha", 3L, GAME_ID);

        assertEquals(8L, fieldA.getTroops()); // 5 + 3
    }

    @Test
    public void addUnits_zeroTroops_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fieldService.addUnits("Alpha", 0L, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void addUnits_negativeTroops_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fieldService.addUnits("Alpha", -1L, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // --- removeUnits ---

    @Test
    public void removeUnits_validAmount_decreasesTroopCount() {
        fieldService.removeUnits("Alpha", 3L, GAME_ID);

        assertEquals(2L, fieldA.getTroops()); // 5 - 3
    }

    @Test
    public void removeUnits_moreThanAvailable_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fieldService.removeUnits("Alpha", 10L, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void removeUnits_zeroTroops_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fieldService.removeUnits("Alpha", 0L, GAME_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // --- countTerritoriesOwnedByPlayer ---

    @Test
    public void countTerritoriesOwnedByPlayer_onlyOwnedFieldsCounted() {
        // playerA owns fieldA only; fieldB is neutral
        int count = fieldService.countTerritoriesOwnedByPlayer(GAME_ID, playerA);

        assertEquals(1, count);
    }

    @Test
    public void countTerritoriesOwnedByPlayer_multipleFields_countedCorrectly() {
        fieldB.setOwner(playerA); // now playerA owns both fields

        int count = fieldService.countTerritoriesOwnedByPlayer(GAME_ID, playerA);

        assertEquals(2, count);
    }

    @Test
    public void countTerritoriesOwnedByPlayer_noOwnedFields_returnsZero() {
        Player outsider = new Player();
        outsider.setPlayerId(99L);

        int count = fieldService.countTerritoriesOwnedByPlayer(GAME_ID, outsider);

        assertEquals(0, count);
    }

    @Test
    public void countTerritoriesOwnedByPlayer_gameNotFound_throwsNotFound() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fieldService.countTerritoriesOwnedByPlayer(GAME_ID, playerA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- helpers ---

    private Field buildField(Long id, String name, Player owner, Long troops) {
        Field f = new Field();
        f.setFieldID(id);
        f.setName(name);
        f.setOwner(owner);
        f.setTroops(troops);
        return f;
    }
}