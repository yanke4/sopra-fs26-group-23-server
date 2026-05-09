package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;

public class LobbyWebSocketDTO {
    private Long lobbyId;
    private LobbyStatus status;
    private Long joinCode;
    private Long hostId;
    private List<Long> jointUserIds;
    private Map<Long, PlayerColor> colorPreferences;

    // Getters and setters
    public Long getLobbyId() { return lobbyId; }
    public void setLobbyId(Long lobbyId) { this.lobbyId = lobbyId; }

    public LobbyStatus getStatus() { return status; }
    public void setStatus(LobbyStatus status) { this.status = status; }

    public Long getJoinCode() { return joinCode; }
    public void setJoinCode(Long joinCode) { this.joinCode = joinCode; }

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }

    public List<Long> getJointUserIds() { return jointUserIds; }
    public void setJointUserIds(List<Long> jointUserIds) { this.jointUserIds = jointUserIds; }

    public Map<Long, PlayerColor> getColorPreferences() { return colorPreferences; }
    public void setColorPreferences(Map<Long, PlayerColor> colorPreferences) {
    this.colorPreferences = colorPreferences;
}
}