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

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public void setMoves(List<Move> moves) {
        this.moves = moves;
    }

    public static class Move{
        private String fromField;
        private Long troops;
        private String toField;

        public String getFromField() {
            return fromField;
        }

        public void setFromField(String fromField) {
            this.fromField = fromField;
        }

        public Long getTroops() {
            return troops;
        }

        public void setTroops(Long troops) {
            this.troops = troops;
        }

        public String getToField() {
            return toField;
        }

        public void setToField(String toField) {
            this.toField = toField;
        }
    }
}
