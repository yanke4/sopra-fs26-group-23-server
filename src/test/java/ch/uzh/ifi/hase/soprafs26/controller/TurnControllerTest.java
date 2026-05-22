package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnAttackDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnDeployDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TurnMoveDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.TurnService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TurnController.class)
public class TurnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TurnService turnService;

    @MockitoBean
    private GameService gameService;

    // --- POST /games/{gameId}/turns/deploy ---

    @Test
    public void deploy_validRequest_returnsOk() throws Exception {
        TurnDeployDTO turnDeployDTO = new TurnDeployDTO();
        turnDeployDTO.setPlayerId(1L);
        turnDeployDTO.setDeployments(Collections.emptyList());

        doNothing().when(turnService).deployUnits(any(TurnDeployDTO.class), eq(1L));

        mockMvc.perform(post("/games/1/turns/deploy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(turnDeployDTO)))
                .andExpect(status().isOk());
    }

    @Test
    public void deploy_wrongPlayer_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn."))
                .when(turnService).deployUnits(any(TurnDeployDTO.class), eq(1L));

        TurnDeployDTO dto = new TurnDeployDTO();
        dto.setPlayerId(99L);
        dto.setDeployments(Collections.emptyList());

        mockMvc.perform(post("/games/1/turns/deploy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deploy_wrongPhase_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not the deploy phase."))
                .when(turnService).deployUnits(any(TurnDeployDTO.class), eq(1L));

        TurnDeployDTO dto = new TurnDeployDTO();
        dto.setPlayerId(1L);
        dto.setDeployments(Collections.emptyList());

        mockMvc.perform(post("/games/1/turns/deploy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deploy_gameNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."))
                .when(turnService).deployUnits(any(TurnDeployDTO.class), eq(999L));

        TurnDeployDTO dto = new TurnDeployDTO();
        dto.setPlayerId(1L);
        dto.setDeployments(Collections.emptyList());

        mockMvc.perform(post("/games/999/turns/deploy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // --- POST /games/{gameId}/turns/attack ---

    @Test
    public void attack_validRequest_returnsOk() throws Exception {
        TurnAttackDTO turnAttackDTO = new TurnAttackDTO();
        turnAttackDTO.setPlayerId(1L);

        doNothing().when(turnService).attack(any(TurnAttackDTO.class), eq(1L));

        mockMvc.perform(post("/games/1/turns/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(turnAttackDTO)))
                .andExpect(status().isOk());
    }

    @Test
    public void attack_wrongPlayer_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn."))
                .when(turnService).attack(any(TurnAttackDTO.class), eq(1L));

        TurnAttackDTO dto = new TurnAttackDTO();
        dto.setPlayerId(99L);

        mockMvc.perform(post("/games/1/turns/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void attack_wrongPhase_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not the attack phase."))
                .when(turnService).attack(any(TurnAttackDTO.class), eq(1L));

        TurnAttackDTO dto = new TurnAttackDTO();
        dto.setPlayerId(1L);

        mockMvc.perform(post("/games/1/turns/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // --- POST /games/{gameId}/turns/move ---

    @Test
    public void move_validRequest_returnsOk() throws Exception {
        TurnMoveDTO turnMoveDTO = new TurnMoveDTO();
        turnMoveDTO.setPlayerId(1L);

        doNothing().when(turnService).moveUnits(any(TurnMoveDTO.class), eq(1L));

        mockMvc.perform(post("/games/1/turns/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(turnMoveDTO)))
                .andExpect(status().isOk());
    }

    @Test
    public void move_wrongPlayer_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn."))
                .when(turnService).moveUnits(any(TurnMoveDTO.class), eq(1L));

        TurnMoveDTO dto = new TurnMoveDTO();
        dto.setPlayerId(99L);

        mockMvc.perform(post("/games/1/turns/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void move_alreadyMovedThisTurn_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Move already done this turn."))
                .when(turnService).moveUnits(any(TurnMoveDTO.class), eq(1L));

        TurnMoveDTO dto = new TurnMoveDTO();
        dto.setPlayerId(1L);

        mockMvc.perform(post("/games/1/turns/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void move_wrongPhase_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not the fortify phase."))
                .when(turnService).moveUnits(any(TurnMoveDTO.class), eq(1L));

        TurnMoveDTO dto = new TurnMoveDTO();
        dto.setPlayerId(1L);

        mockMvc.perform(post("/games/1/turns/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // --- POST /games/{gameId}/turns/advance-phase ---

    @Test
    public void advancePhase_validRequest_returnsOk() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("playerId", 1L);

        doNothing().when(gameService).advancePhase(eq(1L), eq(1L));

        mockMvc.perform(post("/games/1/turns/advance-phase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    public void advancePhase_wrongPlayer_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn."))
                .when(gameService).advancePhase(eq(1L), eq(99L));

        Map<String, Long> body = new HashMap<>();
        body.put("playerId", 99L);

        mockMvc.perform(post("/games/1/turns/advance-phase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void advancePhase_undeployedTroops_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deploy all troops first."))
                .when(gameService).advancePhase(eq(1L), eq(1L));

        Map<String, Long> body = new HashMap<>();
        body.put("playerId", 1L);

        mockMvc.perform(post("/games/1/turns/advance-phase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void advancePhase_gameNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."))
                .when(gameService).advancePhase(eq(999L), any());

        Map<String, Long> body = new HashMap<>();
        body.put("playerId", 1L);

        mockMvc.perform(post("/games/999/turns/advance-phase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }
}