package ch.uzh.ifi.hase.soprafs26.rest.dto;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.MissionStatus;
import ch.uzh.ifi.hase.soprafs26.constant.MissionType;
import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;

public class GameStateDTO {
    private Long gameId;
    private GameStatus status;
    private int currentPlayerIndex;
    private Long currentPlayerId;
    private GamePhase currentPhase;
    private List<PlayerStateDTO> players;
    private List<FieldStateDTO> fields;
    private AttackEventDTO lastAttack;
    private boolean moveDoneThisTurn;
    private int turnNumber;
    private Integer turnTimerSeconds;
    private Long turnStartedAtMillis;
    private Long timedOutPlayerId; // one-shot: set on the broadcast that follows a forced turn end
    private boolean fogOfWarEnabled;

    public boolean isMoveDoneThisTurn() { return moveDoneThisTurn; }
    public void setMoveDoneThisTurn(boolean moveDoneThisTurn) { this.moveDoneThisTurn = moveDoneThisTurn; }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    public Integer getTurnTimerSeconds() { return turnTimerSeconds; }
    public void setTurnTimerSeconds(Integer turnTimerSeconds) { this.turnTimerSeconds = turnTimerSeconds; }

    public Long getTurnStartedAtMillis() { return turnStartedAtMillis; }
    public void setTurnStartedAtMillis(Long turnStartedAtMillis) { this.turnStartedAtMillis = turnStartedAtMillis; }

    public Long getTimedOutPlayerId() { return timedOutPlayerId; }

    public void setTimedOutPlayerId(Long timedOutPlayerId) {this.timedOutPlayerId = timedOutPlayerId;}
    
    public boolean isFogOfWarEnabled() { return fogOfWarEnabled; }
    public void setFogOfWarEnabled(boolean fogOfWarEnabled) { this.fogOfWarEnabled = fogOfWarEnabled; }

    public static class PlayerStateDTO {
        private Long playerId;
        private Long userId;
        private String username;
        private PlayerColor color;
        private boolean alive;
        private Long troopCount;
        private MissionType missionType;
        private String missionDescription;
        private MissionStatus missionStatus;
        private int missionStartRound;
        private int missionExpiresAtRound;
        private int missionBonusTroops;


        public Long getPlayerId() { return playerId; }
        public void setPlayerId(Long playerId) { this.playerId = playerId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) {this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public PlayerColor getColor() { return color; }
        public void setColor(PlayerColor color) { this.color = color; }
        public boolean isAlive() { return alive; }
        public void setAlive(boolean alive) { this.alive = alive; }
        public Long getTroopCount() { return troopCount; }
        public void setTroopCount(Long troopCount) { this.troopCount = troopCount; }
        public MissionType getMissionType() { return missionType; }
        public void setMissionType(MissionType missionType) { this.missionType = missionType; }
        public String getMissionDescription() { return missionDescription; }
        public void setMissionDescription(String missionDescription) { this.missionDescription = missionDescription; }
        public MissionStatus getMissionStatus() { return missionStatus; }
        public void setMissionStatus(MissionStatus missionStatus) { this.missionStatus = missionStatus; }
        public int getMissionStartRound() { return missionStartRound; }
        public void setMissionStartRound(int missionStartRound) { this.missionStartRound = missionStartRound; }
        public int getMissionExpiresAtRound() { return missionExpiresAtRound; }
        public void setMissionExpiresAtRound(int missionExpiresAtRound) { this.missionExpiresAtRound = missionExpiresAtRound; }
        public int getMissionBonusTroops() { return missionBonusTroops; }
        public void setMissionBonusTroops(int missionBonusTroops) { this.missionBonusTroops = missionBonusTroops; }

    }

    public static class FieldStateDTO {
        private String fieldName;
        private Long ownerPlayerId; //null if unoccupied
        private Long troops;

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public Long getOwnerPlayerId() { return ownerPlayerId; }
        public void setOwnerPlayerId(Long ownerPlayerId) { this.ownerPlayerId = ownerPlayerId; }
        public Long getTroops() { return troops; }
        public void setTroops(Long troops) { this.troops = troops; }
    }

    public static class AttackEventDTO {
        private String attacker;
        private String defender;
        private Long attackerLosses;
        private Long defenderLosses;
        private boolean conquered;

        public String getAttacker() { return attacker; }
        public void setAttacker(String attacker) { this.attacker = attacker; }
        public String getDefender() { return defender; }
        public void setDefender(String defender) { this.defender = defender; }
        public Long getAttackerLosses() { return attackerLosses; }
        public void setAttackerLosses(Long attackerLosses) { this.attackerLosses = attackerLosses; }
        public Long getDefenderLosses() { return defenderLosses; }
        public void setDefenderLosses(Long defenderLosses) { this.defenderLosses = defenderLosses; }
        public boolean isConquered() { return conquered; }
        public void setConquered(boolean conquered) { this.conquered = conquered; }
    }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }
    public Long getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(Long currentPlayerId) { this.currentPlayerId = currentPlayerId; }
    public GamePhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(GamePhase currentPhase) { this.currentPhase = currentPhase; }
    public List<PlayerStateDTO> getPlayers() { return players; }
    public void setPlayers(List<PlayerStateDTO> players) { this.players = players; }
    public List<FieldStateDTO> getFields() { return fields; }
    public void setFields(List<FieldStateDTO> fields) { this.fields = fields; }
    public AttackEventDTO getLastAttack() { return lastAttack; }
    public void setLastAttack(AttackEventDTO lastAttack) { this.lastAttack = lastAttack; }
}
