package ch.uzh.ifi.hase.soprafs26.controller;

import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

@RestController
public class DebugController {

    private static final Instant SERVER_STARTED_AT = Instant.now();

    private final Environment env;
    private final DataSource dataSource;
    private final UserRepository userRepository;
    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;

    DebugController(Environment env,
                    DataSource dataSource,
                    UserRepository userRepository,
                    LobbyRepository lobbyRepository,
                    GameRepository gameRepository) {
        this.env = env;
        this.dataSource = dataSource;
        this.userRepository = userRepository;
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
    }

    @GetMapping("/debug/info")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activeProfiles", env.getActiveProfiles());
        body.put("serverStartedAt", SERVER_STARTED_AT.toString());
        body.put("now", Instant.now().toString());

        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection c = dataSource.getConnection()) {
            db.put("product", c.getMetaData().getDatabaseProductName());
            db.put("version", c.getMetaData().getDatabaseProductVersion());
            db.put("url", c.getMetaData().getURL());
        } catch (Exception e) {
            db.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        body.put("database", db);

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("users", userRepository.count());
        counts.put("lobbies", lobbyRepository.count());
        counts.put("games", gameRepository.count());
        body.put("counts", counts);

        return body;
    }
}
