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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long regionID;

    private String name;

    private int bonusAmount;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBonusAmount() {
        return bonusAmount;
    }

    public void setBonusAmount(int bonusAmount) {
        this.bonusAmount = bonusAmount;
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
