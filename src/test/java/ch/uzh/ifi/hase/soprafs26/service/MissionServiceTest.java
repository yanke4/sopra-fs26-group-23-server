package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.MissionType;
import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Map;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MissionServiceTest {

    private MissionService missionService;
    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        missionService = new MissionService();
        player = new Player();
        player.setPlayerId(1L);
        player.setAlive(true);
        game = new Game();
        game.setTurnNumber(3);
        game.setPlayerOrder(List.of(player));
    }

    /** A region whose fields are all owned by the given player. isOwnedBy() will return true. */
    private Region ownedRegion(Player owner, String... fieldNames) {
        Region r = new Region();
        r.setFields(List.of(fieldNames).stream().map(name -> {
            Field f = new Field();
            f.setName(name);
            f.setOwner(owner);
            f.setTroops(5L);
            return f;
        }).toList());
        return r;
    }

    /** A field with a specific troop count, owned by player. */
    private Field ownedField(String name, long troops) {
        Field f = new Field();
        f.setName(name);
        f.setOwner(player);
        f.setTroops(troops);
        return f;
    }

    private void setMap(Region... regions) {
        Map map = new Map();
        map.setRegions(List.of(regions));
        game.setMap(map);
    }

    private void forceMission(MissionType type) {
        player.setCurrentMissionType(type);
        player.setMissionStartRound(3);
        player.setMissionCompleted(false);
        player.setMissionActiveSnapshotTaken(true);
        player.setMissionEliminationsSnapshot(0);
        player.setMissionPeacefulSnapshot(0);
    }

    @Test
    void assignInitialMission_setsCorrectInitialState() {
        missionService.assignInitialMission(player, game);
        assertNotNull(player.getCurrentMissionType());
        assertNull(player.getLastMissionType());
        assertEquals(3, player.getMissionStartRound());
        assertFalse(player.isMissionCompleted());
        assertFalse(player.isMissionActiveSnapshotTaken());
    }

    @Test
    void assignInitialMission_excludesInvalidMissions() {
        // 1 alive player → ELIMINATE_PLAYER excluded; owns Iceland → CONQUER_ICELAND excluded
        setMap(ownedRegion(player, "Iceland", "Reykjavik"));
        for (int i = 0; i < 30; i++) {
            missionService.assignInitialMission(player, game);
            assertNotEquals(MissionType.CONQUER_ICELAND, player.getCurrentMissionType());
            assertNotEquals(MissionType.ELIMINATE_PLAYER, player.getCurrentMissionType());
        }
    }

    @Test
    void rotateIfDue_rotatesOnCycleBoundaryAndCatchesUpMultipleCycles() {
        missionService.assignInitialMission(player, game); // startRound = 3
        game.setTurnNumber(5);
        missionService.rotateIfDue(player, game);
        assertEquals(3, player.getMissionStartRound()); // not yet due

        game.setTurnNumber(12); // three full cycles elapsed
        missionService.rotateIfDue(player, game);
        assertEquals(12, player.getMissionStartRound());
        assertFalse(player.isMissionCompleted());
        assertFalse(player.isMissionActiveSnapshotTaken());
    }

    @Test
    void evaluateAndReward_guardsAgainstInvalidState() {
        player.setAlive(false);
        forceMission(MissionType.CONTROL_TWO_REGIONS);
        assertFalse(missionService.evaluateAndReward(player, game));   // dead
        assertFalse(missionService.evaluateAndReward(null, game));     // null
        player.setAlive(true);
        game.setTurnNumber(2);                                         // locked (before round 3)
        assertFalse(missionService.evaluateAndReward(player, game));
    }

    @Test
    void evaluateAndReward_controlTwoRegions_rewardsCorrectBonus() {
        forceMission(MissionType.CONTROL_TWO_REGIONS);
        // Two fully-owned regions (each has multiple fields all owned by player)
        setMap(ownedRegion(player, "France", "Paris"), ownedRegion(player, "Germany", "Berlin"));
        assertTrue(missionService.evaluateAndReward(player, game));
        assertTrue(player.isMissionCompleted());
        assertEquals(MissionType.CONTROL_TWO_REGIONS.getBonusTroops(), player.getPendingMissionBonus());
    }

    @Test
    void evaluateAndReward_counterBasedMissions() {
        // ELIMINATE_PLAYER
        forceMission(MissionType.ELIMINATE_PLAYER);
        player.setEliminationsCaused(1);
        assertTrue(missionService.evaluateAndReward(player, game));

        // KILL_EIGHT_TROOPS: 7 fails, 8 passes
        forceMission(MissionType.KILL_EIGHT_TROOPS_IN_TURN);
        player.setTroopsKilledThisTurn(7);
        assertFalse(missionService.evaluateAndReward(player, game));
        forceMission(MissionType.KILL_EIGHT_TROOPS_IN_TURN);
        player.setTroopsKilledThisTurn(8);
        assertTrue(missionService.evaluateAndReward(player, game));

        // NO_ATTACK
        forceMission(MissionType.NO_ATTACK_THIS_TURN);
        player.setPeacefulTurnsCompleted(1);
        assertTrue(missionService.evaluateAndReward(player, game));

        // CONQUER_FIVE: 4 fails, 5 passes
        forceMission(MissionType.CONQUER_FIVE_TERRITORIES_IN_TURN);
        player.setTerritoriesConqueredThisTurn(4);
        assertFalse(missionService.evaluateAndReward(player, game));
        forceMission(MissionType.CONQUER_FIVE_TERRITORIES_IN_TURN);
        player.setTerritoriesConqueredThisTurn(5);
        assertTrue(missionService.evaluateAndReward(player, game));
    }

    @Test
    void evaluateAndReward_mapBasedMissions() {
        // FIFTEEN_TROOPS: needs a field with 15 troops owned by player
        forceMission(MissionType.FIFTEEN_TROOPS_ON_TERRITORY);
        Region r = new Region();
        r.setFields(List.of(ownedField("BigStack", 15)));
        setMap(r);
        assertTrue(missionService.evaluateAndReward(player, game));

        // HOLD_REGION_WITH_THREE_TROOPS: all fields in a fully-owned region must have >= 3 troops
        forceMission(MissionType.HOLD_REGION_WITH_THREE_TROOPS);
        Region held = new Region();
        held.setFields(List.of(ownedField("A", 3), ownedField("B", 4)));
        setMap(held);
        assertTrue(missionService.evaluateAndReward(player, game));

        // Territory conquests: the named field just needs to exist and be owned by player
        for (var pair : List.of(
                new Object[]{MissionType.CONQUER_ICELAND,  "Iceland"},
                new Object[]{MissionType.CONQUER_TURKEY,   "Turkey"},
                new Object[]{MissionType.CONQUER_PORTUGAL, "Portugal"})) {
            forceMission((MissionType) pair[0]);
            Region tr = new Region();
            tr.setFields(List.of(ownedField((String) pair[1], 3)));
            setMap(tr);
            assertTrue(missionService.evaluateAndReward(player, game), pair[1] + " should satisfy");
        }
    }

    @Test
    void evaluateAndReward_snapshotPreventsPreActivationCredit() {
        forceMission(MissionType.ELIMINATE_PLAYER);
        player.setEliminationsCaused(2);
        player.setMissionActiveSnapshotTaken(false); // force snapshot to be taken on this call
        missionService.evaluateAndReward(player, game);
        // Snapshot captures 2; no new elimination → not satisfied
        assertFalse(player.isMissionCompleted());
        assertTrue(player.isMissionActiveSnapshotTaken());
    }

    @Test
    void evaluateAndReward_nullMap_doesNotThrow() {
        forceMission(MissionType.CONTROL_TWO_REGIONS);
        game.setMap(null);
        assertDoesNotThrow(() -> missionService.evaluateAndReward(player, game));
    }
}