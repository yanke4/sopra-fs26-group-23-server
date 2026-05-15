package ch.uzh.ifi.hase.soprafs26.constant;

public enum MissionType {
    CONTROL_TWO_REGIONS(2, "Control two full regions"),
    ELIMINATE_PLAYER(2, "Eliminate a player from the game"),
    KILL_EIGHT_TROOPS_IN_TURN(2, "Kill 8 enemy troops in one of your turns"),
    FIFTEEN_TROOPS_ON_TERRITORY(2, "Hold at least 15 troops on a single territory"),
    HOLD_REGION_WITH_THREE_TROOPS(2, "Control a full region with 3+ troops on every territory"),
    CONQUER_ICELAND(2, "Conquer Iceland"),
    CONQUER_TURKEY(2, "Conquer Turkey"),
    CONQUER_PORTUGAL(2, "Conquer Portugal"),
    NO_ATTACK_THIS_TURN(2, "Do not attack any territory for a full turn"),
    CONQUER_FIVE_TERRITORIES_IN_TURN(2, "Conquer 5 territories in a single turn");

    private final int bonusTroops;
    private final String description;

    MissionType(int bonusTroops, String description) {
        this.bonusTroops = bonusTroops;
        this.description = description;
    }

    public int getBonusTroops() {
        return bonusTroops;
    }

    public String getDescription() {
        return description;
    }
}
