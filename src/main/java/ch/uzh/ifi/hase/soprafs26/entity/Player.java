package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.MissionStatus;
import ch.uzh.ifi.hase.soprafs26.constant.MissionType;
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

    @Enumerated(EnumType.STRING)
    @Column
    private MissionType currentMissionType;

    @Enumerated(EnumType.STRING)
    @Column
    private MissionType lastMissionType;

    @Column(nullable = false)
    private int missionStartRound = 3;

    @Column(nullable = false)
    private boolean missionCompleted = false;

    @Column(nullable = false)
    private long troopsKilledThisTurn = 0L;

    @Column(nullable = false)
    private int eliminationsCaused = 0;

    @Column(nullable = false)
    private int missionEliminationsSnapshot = 0;

    @Column(nullable = false)
    private long pendingMissionBonus = 0L;

    @Column(nullable = false)
    private boolean hasAttackedThisTurn = false;

    @Column(nullable = false)
    private int peacefulTurnsCompleted = 0;

    @Column(nullable = false)
    private int missionPeacefulSnapshot = 0;

    @Column(nullable = false)
    private int territoriesConqueredThisTurn = 0;

    @Column(nullable = false)
    private boolean missionActiveSnapshotTaken = false;

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

    public Game getGame() { return game; }

    public MissionType getCurrentMissionType() { return currentMissionType; }
    public void setCurrentMissionType(MissionType currentMissionType) { this.currentMissionType = currentMissionType; }

    public MissionType getLastMissionType() { return lastMissionType; }
    public void setLastMissionType(MissionType lastMissionType) { this.lastMissionType = lastMissionType; }

    public int getMissionStartRound() { return missionStartRound; }
    public void setMissionStartRound(int missionStartRound) { this.missionStartRound = missionStartRound; }

    public boolean isMissionCompleted() { return missionCompleted; }
    public void setMissionCompleted(boolean missionCompleted) { this.missionCompleted = missionCompleted; }

    public long getTroopsKilledThisTurn() { return troopsKilledThisTurn; }
    public void setTroopsKilledThisTurn(long troopsKilledThisTurn) { this.troopsKilledThisTurn = troopsKilledThisTurn; }

    public int getEliminationsCaused() { return eliminationsCaused; }
    public void setEliminationsCaused(int eliminationsCaused) { this.eliminationsCaused = eliminationsCaused; }

    public int getMissionEliminationsSnapshot() { return missionEliminationsSnapshot; }
    public void setMissionEliminationsSnapshot(int missionEliminationsSnapshot) { this.missionEliminationsSnapshot = missionEliminationsSnapshot; }

    public long getPendingMissionBonus() { return pendingMissionBonus; }
    public void setPendingMissionBonus(long pendingMissionBonus) { this.pendingMissionBonus = pendingMissionBonus; }

    public boolean isHasAttackedThisTurn() { return hasAttackedThisTurn; }
    public void setHasAttackedThisTurn(boolean hasAttackedThisTurn) { this.hasAttackedThisTurn = hasAttackedThisTurn; }

    public int getPeacefulTurnsCompleted() { return peacefulTurnsCompleted; }
    public void setPeacefulTurnsCompleted(int peacefulTurnsCompleted) { this.peacefulTurnsCompleted = peacefulTurnsCompleted; }

    public int getMissionPeacefulSnapshot() { return missionPeacefulSnapshot; }
    public void setMissionPeacefulSnapshot(int missionPeacefulSnapshot) { this.missionPeacefulSnapshot = missionPeacefulSnapshot; }

    public int getTerritoriesConqueredThisTurn() { return territoriesConqueredThisTurn; }
    public void setTerritoriesConqueredThisTurn(int territoriesConqueredThisTurn) { this.territoriesConqueredThisTurn = territoriesConqueredThisTurn; }

    public boolean isMissionActiveSnapshotTaken() { return missionActiveSnapshotTaken; }
    public void setMissionActiveSnapshotTaken(boolean missionActiveSnapshotTaken) { this.missionActiveSnapshotTaken = missionActiveSnapshotTaken; }

    public MissionStatus computeMissionStatus(int currentRound) {
        if (currentMissionType == null) return MissionStatus.LOCKED;
        if (currentRound < missionStartRound) return MissionStatus.LOCKED;
        if (missionCompleted) return MissionStatus.COMPLETED;
        return MissionStatus.ACTIVE;
    }
}
