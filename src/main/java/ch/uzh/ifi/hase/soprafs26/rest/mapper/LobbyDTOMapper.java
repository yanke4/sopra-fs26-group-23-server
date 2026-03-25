package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;

@Mapper(uses = {UserDTOMapper.class})
public interface LobbyDTOMapper {
    LobbyDTOMapper INSTANCE = Mappers.getMapper(LobbyDTOMapper.class);

    // LobbyPostDTO is handled in LobbyService, so no mapping is needed

    LobbyGetDTO convertEntityToLobbyGetDTO(Lobby lobby);

    // LobbyPutDTO is handled in LobbyService, so no mapping is needed
}
