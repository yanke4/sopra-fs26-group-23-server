package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "FELD") 
public class Field implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fieldID;

    private String name; 
    private Long troops; 
    @ManyToOne
    private Player owner; 
    @ManyToOne
    private Region region;
    @ManyToMany
    private List<Field> neighbours; 


    public void addTroops(Long troopsToAdd){
    if (troopsToAdd == null) return; 
    if (this.troops == null){
        this.troops = troopsToAdd;
        }
    else {
        this.troops += troopsToAdd;
        }
    }

    public void removeTroops(Long troopsToRemove){
        if (troopsToRemove == null || this.troops == null) return;
        if (this.troops < troopsToRemove){
            this.troops = 0L;
        }
        else{
        this.troops -= troopsToRemove;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTroops() {
        return troops;
    }

    public void setTroops(Long troops) {
        this.troops = troops;
    }

    public Player getOwner(){
        return owner; 
    }

    public void setOwner(Player owner){
        this.owner = owner; 
    }

    public Region getRegion() {
        return region;
    }  
     
    public void setRegion(Region region) {
        this.region = region;
    }

    public List<Field> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(List<Field> neighbours) {
        this.neighbours = neighbours;
    }

    public Long getFieldID() {
        return fieldID;
    }

    public void setFieldID(Long fieldID) {
        this.fieldID = fieldID;
    }

}
