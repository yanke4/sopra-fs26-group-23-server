package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @Test
    public void getGameState_existingGame_returns200WithBody() throws Exception {
        GameStateDTO dto = new GameStateDTO();
        dto.setGameId(42L);
        dto.setStatus(GameStatus.RUNNING);
        dto.setCurrentPhase(GamePhase.ATTACK);
        dto.setCurrentPlayerIndex(0);
        dto.setCurrentPlayerId(1L);
        dto.setPlayers(Collections.emptyList());
        dto.setFields(Collections.emptyList());

        given(gameService.getGameState(42L)).willReturn(dto);

        mockMvc.perform(get("/games/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(42)))
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.currentPhase", is("ATTACK")));
    }

    @Test
    public void getGameState_gameNotFound_returns404() throws Exception {
        given(gameService.getGameState(999L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));

        mockMvc.perform(get("/games/999"))
                .andExpect(status().isNotFound());
    }
}