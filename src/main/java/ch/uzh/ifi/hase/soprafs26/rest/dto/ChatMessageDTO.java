package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;

public class ChatMessageDTO {

    private String message;
    private Long playerId; 
    private Long gameId;
    private Long timestamp;
    private String username; 
    private PlayerColor color; 
    
    public String getMessage(){
        return message; 
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getGameId() {
        return gameId;
    }   

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PlayerColor getColor() {
        return color;
    }

    public void setColor(PlayerColor color) {
        this.color = color;
    }

}
