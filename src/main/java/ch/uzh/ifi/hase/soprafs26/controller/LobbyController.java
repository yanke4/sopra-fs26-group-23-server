package ch.uzh.ifi.hase.soprafs26.controller;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.LobbyDTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;

@RestController
public class LobbyController {

    private final LobbyService lobbyService;

    LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }


	@PostMapping("/lobbies")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public LobbyGetDTO createLobby(@RequestBody LobbyPostDTO lobbyPostDTO) {
        Lobby createdLobby = lobbyService.createLobby(lobbyPostDTO.getHostId());
        return LobbyDTOMapper.INSTANCE.convertEntityToLobbyGetDTO(createdLobby);
	}

    @PutMapping("/lobbies/{joinCode}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyGetDTO joinLobby(
        @PathVariable Long joinCode,
        @RequestBody LobbyPutDTO lobbyPutDTO) {
    Lobby lobby = lobbyService.joinLobby(joinCode, lobbyPutDTO.getUserId());
    return LobbyDTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);
    }
}
