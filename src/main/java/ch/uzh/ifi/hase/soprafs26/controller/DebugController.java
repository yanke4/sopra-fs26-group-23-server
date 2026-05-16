package ch.uzh.ifi.hase.soprafs26.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.LobbyDTOMapper;

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

    @GetMapping("/debug/lobby/{id}")
    @Transactional(readOnly = true)
    public Map<String, Object> debugLobby(@PathVariable Long id) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);

        try {
            Lobby lobby = lobbyRepository.findById(id).orElse(null);
            if (lobby == null) {
                body.put("found", false);
                return body;
            }
            body.put("found", true);
            body.put("lobbyId", lobby.getLobbyId());
            body.put("joinCode", lobby.getJoinCode());

            try {
                body.put("rawStatus", String.valueOf(lobby.getStatus()));
            } catch (Throwable t) {
                body.put("rawStatus_error", describe(t));
            }
            try {
                body.put("hostId", lobby.getHost() == null ? null : lobby.getHost().getId());
            } catch (Throwable t) {
                body.put("host_error", describe(t));
            }
            try {
                body.put("jointUsersSize", lobby.getJointUsers() == null ? null : lobby.getJointUsers().size());
            } catch (Throwable t) {
                body.put("jointUsers_error", describe(t));
            }
            try {
                body.put("colorPreferencesSize",
                        lobby.getColorPreferences() == null ? null : lobby.getColorPreferences().size());
            } catch (Throwable t) {
                body.put("colorPreferences_error", describe(t));
            }
            try {
                body.put("turnTimerSeconds", lobby.getTurnTimerSeconds());
            } catch (Throwable t) {
                body.put("turnTimerSeconds_error", describe(t));
            }
            try {
                body.put("fogOfWarEnabled", lobby.isFogOfWarEnabled());
            } catch (Throwable t) {
                body.put("fogOfWarEnabled_error", describe(t));
            }
            try {
                LobbyDTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);
                body.put("mapperOk", true);
            } catch (Throwable t) {
                body.put("mapper_error", describe(t));
            }
        } catch (Throwable t) {
            body.put("fatal_error", describe(t));
        }
        return body;
    }

    private static Map<String, String> describe(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        Map<String, String> m = new LinkedHashMap<>();
        m.put("type", t.getClass().getName());
        m.put("message", String.valueOf(t.getMessage()));
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        m.put("rootType", root.getClass().getName());
        m.put("rootMessage", String.valueOf(root.getMessage()));
        m.put("stack", sw.toString());
        return m;
    }
}
