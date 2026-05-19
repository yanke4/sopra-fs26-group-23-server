package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserStatsRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Qualifier("userStatsRepository")
    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired 
    private PlayerRepository playerRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void setup() {
        gameRepository.deleteAll();
        lobbyRepository.deleteAll();
        userStatsRepository.deleteAll(); // we need to delete user stats first because if we delete a user before then the call on that user stats will fail
        userRepository.deleteAll();
    }

    @Test
    public void createUser_validInputs_success() {
        assertNull(userRepository.findByUsername("testUsername"));

        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPasswordHash("plainPassword");

        User createdUser = userService.createUser(testUser);

        assertEquals(testUser.getId(), createdUser.getId());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertNotEquals("plainPassword", createdUser.getPasswordHash());
        assertNotNull(createdUser.getCreatedAt());
    }

    @Test
    public void createUser_duplicateUsername_throwsException() {
        assertNull(userRepository.findByUsername("testUsername"));

        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPasswordHash("firstPassword");
        userService.createUser(testUser);

        User testUser2 = new User();
        testUser2.setUsername("testUsername");
        testUser2.setPasswordHash("secondPassword");

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
    }
}
