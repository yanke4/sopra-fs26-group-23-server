package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LobbyPutDTO {
    private String lobbyStatus; //used if status is changed

    private Long userId; //used if a user joins or leaves the lobby

    public String getLobbyStatus() {
        return lobbyStatus;
    }
    public void setLobbyStatus(String lobbyStatus) {
        this.lobbyStatus = lobbyStatus;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }   
}
