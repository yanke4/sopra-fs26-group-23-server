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
    public List<Deployment> getDeployments() {
        return deployments;
    }
    
    public static class Deployment{
        private String fieldName;
        private int troops;

        public String getFieldName() {
            return fieldName;
        }

        public int getTroops() {
            return troops;
        }

    }
}
