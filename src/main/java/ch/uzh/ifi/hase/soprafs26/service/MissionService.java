package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.constant.MissionStatus;
import ch.uzh.ifi.hase.soprafs26.constant.MissionType;
import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Region;

@Service
public class MissionService {

    private static final int UNLOCK_ROUND = 3;
    private static final int CYCLE_LENGTH = 3;

    public void assignInitialMission(Player player, Game game) {
        player.setLastMissionType(null);
        player.setCurrentMissionType(pickMission(null, player, game));
        player.setMissionStartRound(UNLOCK_ROUND);
        player.setMissionCompleted(false);
        player.setMissionActiveSnapshotTaken(false);
    }

    /**
     * Rotates a player's mission as many times as needed so its start round matches the current
     * cycle window. Idempotent — calling this every phase advance is cheap.
     */
    public void rotateIfDue(Player player, Game game) {
        int currentRound = game.getTurnNumber();
        if (player.getCurrentMissionType() == null) {
            assignInitialMission(player, game);
        }
        while (currentRound >= player.getMissionStartRound() + CYCLE_LENGTH) {
            MissionType previous = player.getCurrentMissionType();
            player.setLastMissionType(previous);
            player.setCurrentMissionType(pickMission(previous, player, game));
            player.setMissionStartRound(player.getMissionStartRound() + CYCLE_LENGTH);
            player.setMissionCompleted(false);
            player.setMissionActiveSnapshotTaken(false);
        }
    }

    /**
     * Evaluates the player's currently active mission. If satisfied, marks it completed and
     * grants the troop bonus. No-op if the mission is locked, already completed, or null.
     */
    public boolean evaluateAndReward(Player player, Game game) {
        if (player == null || !player.isAlive()) return false;
        int currentRound = game.getTurnNumber();
        if (player.computeMissionStatus(currentRound) != MissionStatus.ACTIVE) return false;

        // Snapshots must reflect state at the moment the mission becomes ACTIVE,
        // not at assignment — otherwise pre-unlock activity (turns 1–2) would
        // count toward missions like ELIMINATE_PLAYER and NO_ATTACK_THIS_TURN.
        if (!player.isMissionActiveSnapshotTaken()) {
            player.setMissionEliminationsSnapshot(player.getEliminationsCaused());
            player.setMissionPeacefulSnapshot(player.getPeacefulTurnsCompleted());
            player.setMissionActiveSnapshotTaken(true);
        }

        MissionType type = player.getCurrentMissionType();
        if (!isSatisfied(type, player, game)) return false;

        player.setMissionCompleted(true);
        player.setPendingMissionBonus(player.getPendingMissionBonus() + type.getBonusTroops());
        return true;
    }

    private boolean isSatisfied(MissionType type, Player player, Game game) {
        switch (type) {
            case CONTROL_TWO_REGIONS:
                return countFullyOwnedRegions(player, game) >= 2;
            case ELIMINATE_PLAYER:
                return player.getEliminationsCaused() > player.getMissionEliminationsSnapshot();
            case KILL_EIGHT_TROOPS_IN_TURN:
                return player.getTroopsKilledThisTurn() >= 8;
            case FIFTEEN_TROOPS_ON_TERRITORY:
                return maxTroopsOnOwnedTerritory(player, game) >= 15;
            case HOLD_REGION_WITH_THREE_TROOPS:
                return hasRegionWithMinTroops(player, game, 3);
            case CONQUER_ICELAND:
                return ownsTerritory(player, game, "Iceland");
            case CONQUER_TURKEY:
                return ownsTerritory(player, game, "Turkey");
            case CONQUER_PORTUGAL:
                return ownsTerritory(player, game, "Portugal");
            case NO_ATTACK_THIS_TURN:
                return player.getPeacefulTurnsCompleted() > player.getMissionPeacefulSnapshot();
            case CONQUER_FIVE_TERRITORIES_IN_TURN:
                return player.getTerritoriesConqueredThisTurn() >= 5;
        }
        return false;
    }

    private boolean ownsTerritory(Player player, Game game, String territoryName) {
        if (game.getMap() == null || game.getMap().getRegions() == null) return false;
        for (Region region : game.getMap().getRegions()) {
            if (region.getFields() == null) continue;
            for (Field field : region.getFields()) {
                if (territoryName.equals(field.getName())
                        && field.getOwner() != null
                        && field.getOwner().getPlayerId().equals(player.getPlayerId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countFullyOwnedRegions(Player player, Game game) {
        if (game.getMap() == null || game.getMap().getRegions() == null) return 0;
        int count = 0;
        for (Region region : game.getMap().getRegions()) {
            if (region.isOwnedBy(player)) count++;
        }
        return count;
    }

    private long maxTroopsOnOwnedTerritory(Player player, Game game) {
        if (game.getMap() == null || game.getMap().getRegions() == null) return 0L;
        long max = 0L;
        for (Region region : game.getMap().getRegions()) {
            if (region.getFields() == null) continue;
            for (Field field : region.getFields()) {
                if (field.getOwner() != null
                        && field.getOwner().getPlayerId().equals(player.getPlayerId())
                        && field.getTroops() != null
                        && field.getTroops() > max) {
                    max = field.getTroops();
                }
            }
        }
        return max;
    }

    private boolean hasRegionWithMinTroops(Player player, Game game, long minTroops) {
        if (game.getMap() == null || game.getMap().getRegions() == null) return false;
        for (Region region : game.getMap().getRegions()) {
            if (!region.isOwnedBy(player)) continue;
            boolean allAboveMin = true;
            for (Field field : region.getFields()) {
                if (field.getTroops() == null || field.getTroops() < minTroops) {
                    allAboveMin = false;
                    break;
                }
            }
            if (allAboveMin) return true;
        }
        return false;
    }

    private MissionType pickMission(MissionType lastType, Player player, Game game) {
        Set<MissionType> excluded = EnumSet.noneOf(MissionType.class);
        if (lastType != null) excluded.add(lastType);

        // Eliminating a player when only 2 remain is identical to winning the game,
        // so it would be either trivial or impossible — skip it in those cases.
        long aliveCount = game.getPlayerOrder() == null
            ? 0
            : game.getPlayerOrder().stream().filter(Player::isAlive).count();
        if (aliveCount <= 2) excluded.add(MissionType.ELIMINATE_PLAYER);

        // Territory-conquest missions are trivial if the player already holds the target.
        if (ownsTerritory(player, game, "Iceland")) excluded.add(MissionType.CONQUER_ICELAND);
        if (ownsTerritory(player, game, "Turkey")) excluded.add(MissionType.CONQUER_TURKEY);
        if (ownsTerritory(player, game, "Portugal")) excluded.add(MissionType.CONQUER_PORTUGAL);

        List<MissionType> pool = new ArrayList<>();
        for (MissionType type : MissionType.values()) {
            if (!excluded.contains(type)) pool.add(type);
        }
        // Safety net: should never be empty with current rules but guard regardless.
        if (pool.isEmpty()) pool.add(lastType != null ? lastType : MissionType.CONTROL_TWO_REGIONS);

        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }
}
