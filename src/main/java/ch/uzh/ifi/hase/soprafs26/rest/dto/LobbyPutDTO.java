package ch.uzh.ifi.hase.soprafs26.rest.dto;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;

public class LobbyPutDTO {
    private LobbyStatus status; //used if status is changed

    private Long userId; //used if a user joins or leaves the lobby

    public LobbyStatus getStatus() {
        return status;
    }
    public void setStatus(LobbyStatus status) {
        this.status = status;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
