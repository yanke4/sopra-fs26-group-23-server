package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;

public class PlayerPostDTO {
    private PlayerColor color;

    public PlayerColor getColor() { return color; }
    public void setColor(PlayerColor color) { this.color = color; }
}
