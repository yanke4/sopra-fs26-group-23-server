package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
@Table(name = "MAP")
public class Map implements Serializable{

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mapID; 

    @OneToMany(mappedBy = "map", cascade = CascadeType.ALL)
    private List<Region> regions; 
    
    @OneToMany
    private List<Field> startPoints;
    
    public Region getRegionByField(Field field){
        if (regions == null || regions.isEmpty()) return null;

        for(Region region : regions){
            if(region.getFields().contains(field)){
                return region;
                }
            }
        return null; 
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;

        if(regions != null){
            for (Region region : regions){
                region.setMap(this);
            }
        }
    }

    public Long getMapID() {
        return mapID;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public List<Field> getStartPoints(){
        return startPoints;
    }

    public void setStartPoints(List<Field> startPoints){
        this.startPoints = startPoints;

    }
    
}
