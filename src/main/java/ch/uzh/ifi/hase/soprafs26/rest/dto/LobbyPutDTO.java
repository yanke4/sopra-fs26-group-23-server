package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LobbyPutDTO {
    private Boolean lobbyStatus; //used if status is changed

    private Long userId; //used if a user joins or leaves the lobby

    public Boolean getLobbyStatus() {
        return lobbyStatus;
    }
    public void setLobbyStatus(Boolean lobbyStatus) {
        this.lobbyStatus = lobbyStatus;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
