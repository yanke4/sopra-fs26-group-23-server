package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

/*
This DTO defines how the attacks are received from the client
should match a body like this:
    {
    "playerId": 1,
    "attacks": [
        {"attackingField": "DEU","troops": 5, "defendingField": "FRA"},
        {"attackingField": "AUT","troops": 3, "defendingField": "ITA"}
        ]
    }
*/
public class TurnAttackDTO {
    private Long playerId;
    private List<Attack> attacks;
    
    public Long getPlayerId() {
        return playerId;
    }
    public List<Attack> getAttacks() {
        return attacks;
    }
    public static class Attack{
        private String attackingField;
        private Long troops;
        private String defendingField;

        public String getAttackingField() {
            return attackingField;
        }
        public Long getTroops() {
            return troops;
        }
        public String getDefendingField() {
            return defendingField;
        }
    }
}
