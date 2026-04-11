package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "PLAYERS")
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", nullable = false)
    private Lobby lobby;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerColor color;

    @Column(nullable = false)
    private Long troopCount = 0L;

    @Column(nullable = false)
    private boolean alive = true;

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Lobby getLobby() { return lobby; }
    public void setLobby(Lobby lobby) { this.lobby = lobby; }

    public PlayerColor getColor() { return color; }
    public void setColor(PlayerColor color) { this.color = color; }

    public Long getTroopCount() { return troopCount; }
    public void setTroopCount(Long troopCount) { this.troopCount = troopCount; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public void setGame(Game game) {
        this.game = game; 
    }
}
