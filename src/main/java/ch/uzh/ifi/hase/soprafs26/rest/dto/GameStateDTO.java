package ch.uzh.ifi.hase.soprafs26.rest.dto;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;

import java.util.List;

public class GameStateDTO {
    private Long gameId;
    private GameStatus status;
    private int currentPlayerIndex;
    private Long currentPlayerId;
    private List<PlayerStateDTO> players;
    private List<FieldStateDTO> fields; 

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

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }
    public Long getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(Long currentPlayerId) { this.currentPlayerId = currentPlayerId; }
    public List<PlayerStateDTO> getPlayers() { return players; }
    public void setPlayers(List<PlayerStateDTO> players) { this.players = players; }
    public List<FieldStateDTO> getFields() { return fields; }
    public void setFields(List<FieldStateDTO> fields) { this.fields = fields; }
}
