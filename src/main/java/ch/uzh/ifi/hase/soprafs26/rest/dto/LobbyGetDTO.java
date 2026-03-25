package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;

public class LobbyGetDTO {
    private Long lobbyId;
    private Boolean lobbyStatus; // true = "open", false = "closed"
    private Long joinCode;
    private UserGetDTO host;
    private List<UserGetDTO> jointUsers;

    public Long getLobbyId() {
        return lobbyId;
    }
    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }
    public Boolean getLobbyStatus() {
        return lobbyStatus;
    }

    public void setLobbyStatus(Boolean lobbyStatus) {
        this.lobbyStatus = lobbyStatus;
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
