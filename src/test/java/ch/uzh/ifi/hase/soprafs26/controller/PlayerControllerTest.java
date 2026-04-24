package ch.uzh.ifi.hase.soprafs26.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.service.PlayerService;

@WebMvcTest(PlayerController.class)
public class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @Test
    public void surrender_validRequest_returns200() throws Exception {
        doNothing().when(playerService).surrender(1L, 10L);

        mockMvc.perform(post("/games/1/players/10/surrender"))
                .andExpect(status().isOk());
    }

    @Test
    public void surrender_gameNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."))
                .when(playerService).surrender(999L, 10L);

        mockMvc.perform(post("/games/999/players/10/surrender"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void surrender_alreadySurrendered_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player has already surrendered."))
                .when(playerService).surrender(1L, 10L);

        mockMvc.perform(post("/games/1/players/10/surrender"))
                .andExpect(status().isBadRequest());
    }
}