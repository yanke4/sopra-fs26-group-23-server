package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GameStartDTO {
    private Long lobbyId;
    private Long gameId;

    public Long getLobbyId() { return lobbyId; }
    public void setLobbyId(Long lobbyId) { this.lobbyId = lobbyId; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
}
