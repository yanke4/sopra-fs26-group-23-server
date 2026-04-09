package ch.uzh.ifi.hase.soprafs26.rest.dto;
import java.util.List;

/*
//This DTO defines how the moves are received from the client
should match a body like this:
    {
    "playerId": 1,
    "moves": [
        {"FromField": "DEU","troops": 5, "ToField": "FRA"},
        {"FromField": "AUT","troops": 3, "ToField": "ITA"}
        ]
    }
*/
public class TurnMoveDTO {
    private Long playerId;
    private List<Move> moves;
    public Long getPlayerId() {
        return playerId;
    }
    public List<Move> getMoves() {
        return moves;
    }

    public static class Move{
        private String fromField;
        private int troops;
        private String toField;

        public String getFromField() {
            return fromField;
        }
        public int getTroops() {
            return troops;
        }
        public String getToField() {
            return toField;
        }
    }
}
