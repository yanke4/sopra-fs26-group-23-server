package ch.uzh.ifi.hase.soprafs26.rest.dto;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;
import ch.uzh.ifi.hase.soprafs26.constant.FogOfWarMode;

public class LobbyPutDTO {
    private LobbyStatus status; //used if status is changed

    private Long userId; //used if a user joins or leaves the lobby

    private Long targetUserId; //used by host to kick a specific member

    private Integer turnTimerSeconds; //used by host to update the lobby's turn timer setting

    private PlayerColor color;

    private FogOfWarMode fogOfWarMode;

    public PlayerColor getColor() { return color; }
    public void setColor(PlayerColor color) { this.color = color; }

    public Integer getTurnTimerSeconds() { return turnTimerSeconds; }
    public void setTurnTimerSeconds(Integer turnTimerSeconds) { this.turnTimerSeconds = turnTimerSeconds; }

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

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public FogOfWarMode getFogOfWarMode() {
        return fogOfWarMode;
    }

    public void setFogOfWarMode(FogOfWarMode fogOfWarMode) {
        this.fogOfWarMode = fogOfWarMode;
    }
}
