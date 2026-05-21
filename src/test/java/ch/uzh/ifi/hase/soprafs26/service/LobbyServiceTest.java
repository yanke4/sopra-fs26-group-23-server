package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.FogOfWarMode;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;

public class LobbyServiceTest {

    private static final Long LOBBY_ID  = 10L;
    private static final Long HOST_ID   = 1L;
    private static final Long GUEST_ID  = 2L;
    private static final Long GUEST2_ID = 3L;

    @Mock private LobbyRepository        lobbyRepository;
    @Mock private UserService            userService;
    @Mock private GameService            gameService;
    @Mock private SimpMessagingTemplate  messagingTemplate;

    private LobbyService lobbyService;
    private Lobby        lobby;
    private User         host;
    private User         guest;
    private User         guest2;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        lobbyService = new LobbyService(lobbyRepository, userService, gameService, messagingTemplate);

        host   = buildUser(HOST_ID,   "host");
        guest  = buildUser(GUEST_ID,  "guest");
        guest2 = buildUser(GUEST2_ID, "guest2");

        lobby = new Lobby();
        lobby.setLobbyId(LOBBY_ID);
        lobby.setStatus(LobbyStatus.OPEN);
        lobby.setJoinCode(123456L);
        lobby.setHost(host);
        lobby.setJointUsers(new ArrayList<>(List.of(guest)));
        lobby.setColorPreferences(new HashMap<>());

        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ======================================================================
    // leaveLobby
    // ======================================================================

    @Test
    public void leaveLobby_guestLeaves_removedFromJointUsers() {
        lobbyService.leaveLobby(LOBBY_ID, GUEST_ID);

        assertTrue(lobby.getJointUsers().isEmpty());
        verify(lobbyRepository).save(lobby);
    }

    @Test
    public void leaveLobby_hostLeavesWithGuests_firstGuestBecomesNewHost() {
        lobbyService.leaveLobby(LOBBY_ID, HOST_ID);

        assertEquals(GUEST_ID, lobby.getHost().getId());
        assertTrue(lobby.getJointUsers().isEmpty());
    }

    @Test
    public void leaveLobby_hostLeavesAloneNoGuests_lobbyIsClosed() {
        lobby.setJointUsers(new ArrayList<>());

        lobbyService.leaveLobby(LOBBY_ID, HOST_ID);

        assertEquals(LobbyStatus.CLOSED, lobby.getStatus());
    }

    @Test
    public void leaveLobby_hostLeavesWithMultipleGuests_onlyFirstIsPromoted() {
        lobby.setJointUsers(new ArrayList<>(List.of(guest, guest2)));

        lobbyService.leaveLobby(LOBBY_ID, HOST_ID);

        assertEquals(GUEST_ID, lobby.getHost().getId());
        assertEquals(1, lobby.getJointUsers().size());
        assertEquals(GUEST2_ID, lobby.getJointUsers().get(0).getId());
    }

    @Test
    public void leaveLobby_lobbyNotFound_throwsNotFound() {
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.leaveLobby(LOBBY_ID, HOST_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ======================================================================
    // kickMember
    // ======================================================================

    @Test
    public void kickMember_hostKicksGuest_guestRemovedFromList() {
        lobbyService.kickMember(LOBBY_ID, HOST_ID, GUEST_ID);

        assertTrue(lobby.getJointUsers().isEmpty());
        verify(lobbyRepository).save(lobby);
    }

    @Test
    public void kickMember_kickedGuestsColorPreferenceAlsoRemoved() {
        lobby.getColorPreferences().put(GUEST_ID, PlayerColor.RED);

        lobbyService.kickMember(LOBBY_ID, HOST_ID, GUEST_ID);

        assertTrue(lobby.getColorPreferences().isEmpty());
    }

    @Test
    public void kickMember_nonHostTriesToKick_throwsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.kickMember(LOBBY_ID, GUEST_ID, HOST_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void kickMember_hostTriesToKickSelf_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.kickMember(LOBBY_ID, HOST_ID, HOST_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void kickMember_nullTargetId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.kickMember(LOBBY_ID, HOST_ID, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void kickMember_targetNotInLobby_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.kickMember(LOBBY_ID, HOST_ID, 999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ======================================================================
    // updateSettings
    // ======================================================================

    @Test
    public void updateSettings_hostSets30SecondTimer_persistedCorrectly() {
        Lobby result = lobbyService.updateSettings(LOBBY_ID, HOST_ID, 30, FogOfWarMode.OFF);

        assertEquals(30, result.getTurnTimerSeconds());
        assertEquals(FogOfWarMode.OFF, result.getFogOfWarMode());
        verify(lobbyRepository).save(lobby);
    }

    @Test
    public void updateSettings_hostSets60SecondTimer_persistedCorrectly() {
        Lobby result = lobbyService.updateSettings(LOBBY_ID, HOST_ID, 60, FogOfWarMode.FULL);

        assertEquals(60, result.getTurnTimerSeconds());
        assertEquals(FogOfWarMode.FULL, result.getFogOfWarMode());
    }

    @Test
    public void updateSettings_hostSetsNullTimer_persistedCorrectly() {
        Lobby result = lobbyService.updateSettings(LOBBY_ID, HOST_ID, null, FogOfWarMode.OFF);

        assertEquals(null, result.getTurnTimerSeconds());
    }

    @Test
    public void updateSettings_invalidTimerValue_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.updateSettings(LOBBY_ID, HOST_ID, 45, FogOfWarMode.OFF));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void updateSettings_nonHostChangesSettings_throwsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.updateSettings(LOBBY_ID, GUEST_ID, 30, FogOfWarMode.OFF));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ======================================================================
    // selectColor
    // ======================================================================

    @Test
    public void selectColor_hostPicksAvailableColor_storedInPreferences() {
        Lobby result = lobbyService.selectColor(LOBBY_ID, HOST_ID, PlayerColor.RED);

        assertEquals(PlayerColor.RED, result.getColorPreferences().get(HOST_ID));
        verify(lobbyRepository).save(lobby);
    }

    @Test
    public void selectColor_guestPicksAvailableColor_storedInPreferences() {
        Lobby result = lobbyService.selectColor(LOBBY_ID, GUEST_ID, PlayerColor.BLUE);

        assertEquals(PlayerColor.BLUE, result.getColorPreferences().get(GUEST_ID));
    }

    @Test
    public void selectColor_colorAlreadyTakenByOtherPlayer_throwsConflict() {
        lobby.getColorPreferences().put(GUEST_ID, PlayerColor.RED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.selectColor(LOBBY_ID, HOST_ID, PlayerColor.RED));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void selectColor_userReclaimsOwnColor_doesNotThrow() {
        lobby.getColorPreferences().put(HOST_ID, PlayerColor.RED);

        // Re-selecting the same color the host already has should not fail
        Lobby result = lobbyService.selectColor(LOBBY_ID, HOST_ID, PlayerColor.RED);

        assertEquals(PlayerColor.RED, result.getColorPreferences().get(HOST_ID));
    }

    @Test
    public void selectColor_outsiderNotInLobby_throwsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.selectColor(LOBBY_ID, 999L, PlayerColor.GREEN));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ======================================================================
    // joinLobby – edge cases not covered by the integration test
    // ======================================================================

    @Test
    public void joinLobby_lobbyIsClosed_throwsConflict() {
        lobby.setStatus(LobbyStatus.CLOSED);
        when(lobbyRepository.findByJoinCode(123456L)).thenReturn(lobby);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(123456L, GUEST2_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void joinLobby_lobbyFull_throwsConflict() {
        // Fill to MAX_PLAYERS-1 (3 guests) so the lobby is already at capacity
        lobby.setJointUsers(new ArrayList<>(List.of(guest,
                buildUser(10L, "p3"), buildUser(11L, "p4"))));
        when(lobbyRepository.findByJoinCode(123456L)).thenReturn(lobby);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(123456L, 99L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void joinLobby_invalidCode_throwsNotFound() {
        when(lobbyRepository.findByJoinCode(000000L)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(000000L, GUEST_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ======================================================================
    // startGame – error paths
    // ======================================================================

    @Test
    public void startGame_notHost_throwsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.startGame(LOBBY_ID, GUEST_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void startGame_notEnoughPlayers_throwsConflict() {
        lobby.setJointUsers(new ArrayList<>()); // host alone

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.startGame(LOBBY_ID, HOST_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private User buildUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }
}