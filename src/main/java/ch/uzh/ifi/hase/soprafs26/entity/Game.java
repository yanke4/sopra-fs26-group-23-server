package ch.uzh.ifi.hase.soprafs26.entity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import java.io.Serializable;


@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    @Id
    private Long id;


    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL) // cascade: when a game gets saved or deleted,the players get saved or deleted as well
    private List<Player> playerOrder;
    
    @OneToOne(cascade = CascadeType.ALL) 
    private Map map;


    @Column 
    private int currentPlayerIndex;

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

}
