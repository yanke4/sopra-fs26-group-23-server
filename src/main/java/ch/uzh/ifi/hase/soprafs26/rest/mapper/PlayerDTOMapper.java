package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerPostDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PlayerDTOMapper {
    PlayerDTOMapper INSTANCE = Mappers.getMapper(PlayerDTOMapper.class);

    @Mapping(source = "color", target = "color")
    @Mapping(target = "playerId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "lobby", ignore = true)
    @Mapping(target = "troopCount", ignore = true)
    @Mapping(target = "alive", ignore = true)
    Player convertPlayerPostDTOtoEntity(PlayerPostDTO playerPostDTO);

    @Mapping(source = "playerId", target = "playerId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "color", target = "color")
    @Mapping(source = "troopCount", target = "troopCount")
    @Mapping(source = "alive", target = "alive")
    PlayerGetDTO convertEntityToPlayerGetDTO(Player player);
}
