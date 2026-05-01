package ch.uzh.ifi.hase.soprafs26.rest.dto;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
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

    public boolean isMoveDoneThisTurn() { return moveDoneThisTurn; }
    public void setMoveDoneThisTurn(boolean moveDoneThisTurn) { this.moveDoneThisTurn = moveDoneThisTurn; }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    public static class PlayerStateDTO {
        private Long playerId;
        private Long userId;
        private String username;
        private PlayerColor color;
        private boolean alive;
        private Long troopCount;
        

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
