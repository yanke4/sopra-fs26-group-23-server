package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "MAP")
public class Map implements Serializable{

    private List<Region> StartPoints;

    public List<Region> getStartPoints() {
        return StartPoints;
    }

    public void setStartPoints(List<Region> startPoints) {
        StartPoints = startPoints;
    }
    
}
