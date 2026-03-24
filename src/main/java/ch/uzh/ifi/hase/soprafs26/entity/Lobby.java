package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.*;
import java.util.ArrayList;


@Entity
@Table(name = "lobbies")
public class Lobby implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
	@GeneratedValue
    private Long lobbyId;

    @Column(nullable = false)
    private String lobbyStatus; // "open", "closed", "in-game"

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
    public String getLobbyStatus() {
        return lobbyStatus;
    }
    public void setLobbyStatus(String lobbyStatus) {
        this.lobbyStatus = lobbyStatus;
    }
    public Long getJoinCode() {
        return JoinCode;
    }
    public void setJoinCode(Long joinCode) {
        JoinCode = joinCode;
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
