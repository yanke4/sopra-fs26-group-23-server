package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStartDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;

@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LobbyService lobbyService;

    private Lobby lobby;
    private User host;

    @BeforeEach
    public void setup() {
        host = new User();
        host.setId(1L);
        host.setUsername("hostUser");

        lobby = new Lobby();
        lobby.setLobbyId(10L);
        lobby.setStatus(LobbyStatus.OPEN);
        lobby.setJoinCode(123456L);
        lobby.setHost(host);
        lobby.setJointUsers(Collections.emptyList());
    }

    // --- POST /lobbies ---

    @Test
    public void createLobby_validRequest_returns201WithLobbyId() throws Exception {
        given(lobbyService.createLobby(1L)).willReturn(lobby);

        mockMvc.perform(post("/lobbies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostId\": 1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lobbyId", is(10)))
                .andExpect(jsonPath("$.joinCode", is(123456)));
    }

    @Test
    public void createLobby_hostNotFound_returns404() throws Exception {
        given(lobbyService.createLobby(any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        mockMvc.perform(post("/lobbies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostId\": 999}"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /lobbies/{joinCode} (join) ---

    @Test
    public void joinLobby_validRequest_returns200() throws Exception {
        given(lobbyService.joinLobby(123456L, 2L)).willReturn(lobby);

        mockMvc.perform(put("/lobbies/123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyId", is(10)));
    }

    @Test
    public void joinLobby_lobbyNotFound_returns404() throws Exception {
        given(lobbyService.joinLobby(eq(999999L), any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No lobby found."));

        mockMvc.perform(put("/lobbies/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void joinLobby_lobbyClosed_returns409() throws Exception {
        given(lobbyService.joinLobby(eq(123456L), any()))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is no longer open."));

        mockMvc.perform(put("/lobbies/123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 2}"))
                .andExpect(status().isConflict());
    }

    // --- GET /lobbies/{lobbyId} ---

    @Test
    public void getLobby_existingLobby_returns200() throws Exception {
        given(lobbyService.getLobbyById(10L)).willReturn(lobby);

        mockMvc.perform(get("/lobbies/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyId", is(10)))
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    public void getLobby_notFound_returns404() throws Exception {
        given(lobbyService.getLobbyById(999L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found."));

        mockMvc.perform(get("/lobbies/999"))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /lobbies/{lobbyId}/members/{userId} ---

    @Test
    public void leaveLobby_validRequest_returns204() throws Exception {
        doNothing().when(lobbyService).leaveLobby(10L, 1L);

        mockMvc.perform(delete("/lobbies/10/members/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void leaveLobby_lobbyNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found."))
                .when(lobbyService).leaveLobby(999L, 1L);

        mockMvc.perform(delete("/lobbies/999/members/1"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /lobbies/{lobbyId}/start ---

    @Test
    public void startGame_validRequest_returns200WithGameId() throws Exception {
        GameStartDTO gameStartDTO = new GameStartDTO();
        gameStartDTO.setLobbyId(10L);
        gameStartDTO.setGameId(42L);

        given(lobbyService.startGame(10L, 1L)).willReturn(gameStartDTO);

        mockMvc.perform(put("/lobbies/10/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(42)));
    }

    @Test
    public void startGame_notHost_returns403() throws Exception {
        given(lobbyService.startGame(eq(10L), eq(2L)))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can start the game."));

        mockMvc.perform(put("/lobbies/10/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void startGame_notEnoughPlayers_returns409() throws Exception {
        given(lobbyService.startGame(eq(10L), eq(1L)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "At least 2 players required."));

        mockMvc.perform(put("/lobbies/10/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1}"))
                .andExpect(status().isConflict());
    }
}