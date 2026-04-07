package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;

public class PlayerGetDTO {
    private Long playerId;
    private Long userId;
    private String username;
    private PlayerColor color;
    private int troopCount;
    private boolean alive;

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public PlayerColor getColor() { return color; }
    public void setColor(PlayerColor color) { this.color = color; }

    public int getTroopCount() { return troopCount; }
    public void setTroopCount(int troopCount) { this.troopCount = troopCount; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
}
