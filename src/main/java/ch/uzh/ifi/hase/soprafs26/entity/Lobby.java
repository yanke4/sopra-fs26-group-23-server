package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;


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

    @ElementCollection
    @CollectionTable(name = "LOBBY_COLOR_PREFERENCES", joinColumns = @JoinColumn(name = "lobby_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "color")
    @Enumerated(EnumType.STRING)

    private Map<Long, PlayerColor> colorPreferences = new HashMap<>();

    @Column
    private Integer turnTimerSeconds; // null = no limit

    @Column
    private boolean fogOfWarEnabled;

    public Integer getTurnTimerSeconds() {
        return turnTimerSeconds;
    }
    public void setTurnTimerSeconds(Integer turnTimerSeconds) {
        this.turnTimerSeconds = turnTimerSeconds;
    }

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
    public Map<Long, PlayerColor> getColorPreferences() { 
        return colorPreferences; 
    }

    public void setColorPreferences(Map<Long, PlayerColor> colorPreferences) {
        this.colorPreferences = colorPreferences;
    }
    public boolean isFogOfWarEnabled() {
        return fogOfWarEnabled;
    }
    public void setFogOfWarEnabled(boolean fogOfWarEnabled) {
        this.fogOfWarEnabled = fogOfWarEnabled;
    }
}
