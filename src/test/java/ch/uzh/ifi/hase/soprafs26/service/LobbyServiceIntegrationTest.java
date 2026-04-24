package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStartDTO;

import static org.junit.jupiter.api.Assertions.*;

// @SpringBootTest boots the full application context (real beans, real JPA, real H2)
// @WebAppConfiguration is required because the context includes web/websocket beans
@WebAppConfiguration
@SpringBootTest
public class LobbyServiceIntegrationTest {

    // Real repositories injected from the Spring context — no mocks
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private GameRepository gameRepository;

    // Real services under test, fully wired with their collaborators
    @Autowired
    private UserService userService;

    @Autowired
    private LobbyService lobbyService;

    @BeforeEach
    public void setup() {
        // Wipe tables in child-before-parent order so FK constraints don't block deletion
        gameRepository.deleteAll();
        lobbyRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Helper to reduce fixture noise: creates and persists a user via the real UserService
    private User registerUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash("password-" + username);
        // createUser hashes the password, sets createdAt, and flushes to the DB
        return userService.createUser(u);
    }

    @Test
    public void fullLobbyLifecycle_hostCreatesGuestJoinsHostStarts_success() {
        // Arrange: two real users persisted in the USERS table
        User host = registerUser("host");
        User guest = registerUser("guest");

        // Act 1: host creates a new lobby — hits DB and generates a unique join code
        Lobby created = lobbyService.createLobby(host.getId());

        // Assert: the lobby row was persisted and has an auto-generated id
        assertNotNull(created.getLobbyId());
        // New lobbies must start in the OPEN state so other players can join
        assertEquals(LobbyStatus.OPEN, created.getStatus());
        // A join code must be generated so the guest can find the lobby
        assertNotNull(created.getJoinCode());
        // The creator of the lobby becomes its host
        assertEquals(host.getId(), created.getHost().getId());
        // No guests have joined yet, so the jointUsers list must be empty
        assertTrue(created.getJointUsers().isEmpty());

        // Act 2: guest joins the lobby using the join code just generated
        Lobby joined = lobbyService.joinLobby(created.getJoinCode(), guest.getId());

        // Same lobby row is returned — not a new one
        assertEquals(created.getLobbyId(), joined.getLobbyId());
        // The guest has been appended to jointUsers (size went from 0 to 1)
        assertEquals(1, joined.getJointUsers().size());
        // And the user actually stored is the guest, not someone else
        assertEquals(guest.getId(), joined.getJointUsers().get(0).getId());

        // Act 3: host starts the game — triggers GameService.createGame under the hood
        GameStartDTO dto = lobbyService.startGame(created.getLobbyId(), host.getId());

        // The DTO echoes the lobby that was started
        assertEquals(created.getLobbyId(), dto.getLobbyId());
        // A new Game id has been generated and returned to the caller
        assertNotNull(dto.getGameId());

        // Assert persisted state: reload the lobby from the DB (not the in-memory ref)
        // to prove the transaction actually committed the OPEN → CLOSED transition
        Lobby afterStart = lobbyRepository.findById(created.getLobbyId()).orElseThrow();
        // Once the game has started, the lobby must be closed to new joiners
        assertEquals(LobbyStatus.CLOSED, afterStart.getStatus());
        // And a corresponding Game row must exist in the GAME table
        assertTrue(gameRepository.existsById(dto.getGameId()));
    }
}
