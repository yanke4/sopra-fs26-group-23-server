package ch.uzh.ifi.hase.soprafs26.entity;
import java.io.Serializable;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.FogOfWarMode;
import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;


@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    @Id
    private Long id;

    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long version = 0L;


    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    @jakarta.persistence.OrderColumn(name = "player_order_index")
    private List<Player> playerOrder;
    
    @OneToOne(cascade = CascadeType.ALL) 
    private Map map;


    @Column 
    private int currentPlayerIndex;

    @Column
    private boolean moveDoneThisTurn = false;

    @Column
    private int turnNumber = 1;

    @Column
    private Integer turnTimerSeconds; // null = no timer

    @Column
    private Long turnStartedAtMillis; // epoch millis when the current player's turn began

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FogOfWarMode fogOfWarMode = FogOfWarMode.OFF;


    @Enumerated(EnumType.STRING)
    private GameStatus status; //either waiting, running or finished

    @Enumerated(EnumType.STRING)
    private GamePhase currentPhase;

    public Player getCurrentPlayer(){
        if(playerOrder == null || playerOrder.isEmpty()) {
            return null;
        }
        return playerOrder.get(currentPlayerIndex);
    }

    public void setPlayerOrder(List<Player> playerOrder) {
        this.playerOrder = playerOrder;

        if(playerOrder != null) {
            for (Player player : playerOrder) {
                player.setGame(this);
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Player> getPlayerOrder() {
        return playerOrder;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(GamePhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public boolean isMoveDoneThisTurn() {
        return moveDoneThisTurn;
    }

public void setMoveDoneThisTurn(boolean moveDoneThisTurn) {
        this.moveDoneThisTurn = moveDoneThisTurn;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setTurnNumber(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    public Integer getTurnTimerSeconds() {
        return turnTimerSeconds;
    }

    public void setTurnTimerSeconds(Integer turnTimerSeconds) {
        this.turnTimerSeconds = turnTimerSeconds;
    }

    public Long getTurnStartedAtMillis() {
        return turnStartedAtMillis;
    }

    public void setTurnStartedAtMillis(Long turnStartedAtMillis) {
        this.turnStartedAtMillis = turnStartedAtMillis;
    }

    public FogOfWarMode getFogOfWarMode() {
        return fogOfWarMode;
    }
    public void setFogOfWarMode(FogOfWarMode fogOfWarMode) {
        this.fogOfWarMode = fogOfWarMode;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
