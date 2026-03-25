package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import jakarta.persistence.*;
import java.util.ArrayList;


@Entity
@Table(name = "LOBBIES")
public class Lobby implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
	@GeneratedValue
    private Long lobbyId;

    @Column(nullable = false)
    private LobbyStatus status; // OPEN, CLOSED

    @Column(nullable = false, unique = true)
    private Long joinCode;

    @ManyToOne
    private User host;

    @ManyToMany
    private List<User> jointUsers = new ArrayList<>();

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
    public User getHost() {
        return host;
    }
    public void setHost(User host) {
        this.host = host;
    }
    public List<User> getJointUsers() {
        return jointUsers;
    }
    public void setJointUsers(List<User> jointUsers) {
        this.jointUsers = jointUsers;
    }
}
