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

public class RegionServiceTest {

    private static final Long GAME_ID = 50L;

    @Mock
    private GameRepository gameRepository;

    private RegionService regionService;

    private Player player;
    private Game game;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        regionService = new RegionService(gameRepository);

        player = new Player();
        player.setPlayerId(1L);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Region whose every field is owned by the given player. */
    private Region fullyOwnedRegion(int bonus, Player owner, String... fieldNames) {
        Region r = new Region();
        r.setBonusAmount(bonus);
        List<Field> fields = new ArrayList<>();
        for (String name : fieldNames) {
            Field f = new Field();
            f.setName(name);
            f.setOwner(owner);
            f.setTroops(1L);
            fields.add(f);
        }
        r.setFields(fields);
        return r;
    }

    /** Region with one field owned by owner1 and one owned by owner2 (partial). */
    private Region partialRegion(int bonus, Player owner1, Player owner2) {
        Region r = new Region();
        r.setBonusAmount(bonus);
        Field f1 = new Field(); f1.setOwner(owner1); f1.setName("F1");
        Field f2 = new Field(); f2.setOwner(owner2); f2.setName("F2");
        r.setFields(List.of(f1, f2));
        return r;
    }

    private void setupGame(Region... regions) {
        Map map = new Map();
        map.setRegions(List.of(regions));
        game = new Game();
        game.setId(GAME_ID);
        game.setMap(map);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
    }

    // -----------------------------------------------------------------------
    // calculateRegionBonus
    // -----------------------------------------------------------------------

    @Test
    public void calculateRegionBonus_ownsNoRegion_returnsZero() {
        Player other = new Player();
        other.setPlayerId(2L);
        setupGame(fullyOwnedRegion(5, other, "A", "B")); // player owns nothing

        int bonus = regionService.calculateRegionBonus(GAME_ID, player);

        assertEquals(0, bonus);
    }

    @Test
    public void calculateRegionBonus_ownsOneCompleteRegion_returnsThatBonus() {
        setupGame(fullyOwnedRegion(3, player, "France", "Paris"));

        int bonus = regionService.calculateRegionBonus(GAME_ID, player);

        assertEquals(3, bonus);
    }

    @Test
    public void calculateRegionBonus_ownsMultipleRegions_sumsAllBonuses() {
        setupGame(
                fullyOwnedRegion(3, player, "France", "Paris"),
                fullyOwnedRegion(5, player, "Germany", "Berlin")
        );

        int bonus = regionService.calculateRegionBonus(GAME_ID, player);

        assertEquals(8, bonus);
    }

    @Test
    public void calculateRegionBonus_partialOwnership_notCounted() {
        Player other = new Player();
        other.setPlayerId(2L);
        // player owns only one of the two fields → region is not fully controlled
        setupGame(partialRegion(4, player, other));

        int bonus = regionService.calculateRegionBonus(GAME_ID, player);

        assertEquals(0, bonus);
    }

    @Test
    public void calculateRegionBonus_mixedOwnership_countsOnlyFullyOwnedRegions() {
        Player other = new Player();
        other.setPlayerId(2L);
        setupGame(
                fullyOwnedRegion(3, player, "Spain"),   // fully owned → counts
                partialRegion(7, player, other)          // partially owned → doesn't count
        );

        int bonus = regionService.calculateRegionBonus(GAME_ID, player);

        assertEquals(3, bonus);
    }

    @Test
    public void calculateRegionBonus_mapIsNull_returnsZero() {
        game = new Game();
        game.setId(GAME_ID);
        game.setMap(null);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        int bonus = regionService.calculateRegionBonus(GAME_ID, player);

        assertEquals(0, bonus);
    }

    @Test
    public void calculateRegionBonus_regionsListIsNull_returnsZero() {
        Map map = new Map();
        map.setRegions(null);
        game = new Game();
        game.setId(GAME_ID);
        game.setMap(map);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        int bonus = regionService.calculateRegionBonus(GAME_ID, player);

        assertEquals(0, bonus);
    }

    @Test
    public void calculateRegionBonus_gameNotFound_throwsNotFound() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> regionService.calculateRegionBonus(GAME_ID, player));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void calculateRegionBonus_nullPlayer_throwsNotFound() {
        setupGame(fullyOwnedRegion(3, player, "France"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> regionService.calculateRegionBonus(GAME_ID, null));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void calculateRegionBonus_playerWithNullId_throwsNotFound() {
        Player noId = new Player(); // playerId is null
        setupGame(fullyOwnedRegion(3, player, "France"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> regionService.calculateRegionBonus(GAME_ID, noId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void calculateRegionBonus_emptyRegions_returnsZero() {
        Map map = new Map();
        map.setRegions(new ArrayList<>());
        game = new Game();
        game.setId(GAME_ID);
        game.setMap(map);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        int bonus = regionService.calculateRegionBonus(GAME_ID, player);

        assertEquals(0, bonus);
    }
}