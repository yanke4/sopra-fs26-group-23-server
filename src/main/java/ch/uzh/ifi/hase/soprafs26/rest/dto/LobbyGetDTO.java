package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;

public class LobbyGetDTO {
    private Long lobbyId;
    private LobbyStatus status; // OPEN, CLOSED
    private Long joinCode;
    private UserGetDTO host;
    private List<UserGetDTO> jointUsers;

    public Long getLobbyId() {
        return lobbyId;
    }
    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }
    public LobbyStatus getStatus() {
        return status;
    }

    public void setStatus(LobbyStatus status) {
        this.status = status;
    }
    public Long getJoinCode() {
        return joinCode;
    }
    public void setJoinCode(Long joinCode) {
        this.joinCode = joinCode;
    }
    public UserGetDTO getHost() {
        return host;
    }
    public void setHost(UserGetDTO host) {
        this.host = host;
    }
    public List<UserGetDTO> getJointUsers() {
        return jointUsers;
    }
    public void setJointUsers(List<UserGetDTO> jointUsers) {
        this.jointUsers = jointUsers;
    }
}
