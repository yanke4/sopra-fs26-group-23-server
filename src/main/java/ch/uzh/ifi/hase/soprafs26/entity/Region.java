package ch.uzh.ifi.hase.soprafs26.entity;
import java.io.Serializable;
import java.util.List;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
@Entity
@Table(name = "REGION")
public class Region implements Serializable {

    private static final int Bonusamount = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long regionID;

    @ManyToOne
    private Map map; 

    @OneToMany(mappedBy ="region", cascade = CascadeType.ALL)
    private List<Field> fields;

    public boolean isOwnedBy(Player player){
        if (player == null || fields == null || fields.isEmpty()) 
            return false;

        for (Field field : fields){
            if (field.getOwner() == null || field.getOwner().getPlayerId() == null ||!field.getOwner().getPlayerId().equals(player.getPlayerId())){
                return false; 
            }
        }
        return true;
    }

    public int getBonusamount() {
        return Bonusamount;
    }

    public Map getMap() {
        return map; 
    }

    public void setMap(Map map){
        this.map = map;
    }

    public List<Field> getFields() {
    return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }
    
    public Long getRegionID() {
        return regionID;
    }

    public void setRegionID(Long regionID) {
        this.regionID = regionID;
    }


}
