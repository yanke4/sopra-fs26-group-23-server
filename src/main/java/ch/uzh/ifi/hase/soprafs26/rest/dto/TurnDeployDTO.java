package ch.uzh.ifi.hase.soprafs26.rest.dto;
import java.util.List;
/*
This DTO defines how the deployments are received from the client
should match a body like this:
    {
    "playerId": 1,
    "deployments": [
        {"fieldName": "DEU","troops": 5},
        {"fieldName": "FRA","troops": 3}
        ]
    }
*/
public class TurnDeployDTO {
    private long playerId; 
    private List<Deployment> deployments;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public List<Deployment> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<Deployment> deployments) {
        this.deployments = deployments;
    }
    
    public static class Deployment{
        private String fieldName;
        private Long troops;

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public Long getTroops() {
            return troops;
        }

        public void setTroops(Long troops) {
            this.troops = troops;
        }

    }
}
