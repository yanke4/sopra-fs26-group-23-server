package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserStats;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserStatsRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserStatsDTO;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Service
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;

    public UserService(@Qualifier("userRepository") UserRepository userRepository, UserStatsRepository userStatsRepository) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
    }

    public List<User> getUsers() {
        return this.userRepository.findAll();
    }

    public User createUser(User newUser) {
        if (newUser.getUsername() == null || newUser.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must not be empty");
        }
        if (newUser.getPasswordHash() == null || newUser.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not be empty");
        }

        checkIfUsernameExists(newUser.getUsername());
        newUser.setPasswordHash(hashPassword(newUser.getPasswordHash()));
        if (newUser.getCreatedAt() == null) {
            newUser.setCreatedAt(Instant.now());
        }

        newUser = userRepository.save(newUser);
        userRepository.flush();

        // Create and save initial statistics for the new user
        UserStats initialStats = new UserStats();
        initialStats.setUser(newUser);
        initialStats.setGamesPlayed(0L);
        initialStats.setWins(0L);
        userStatsRepository.save(initialStats);

        log.debug("Created Information for User: {}", newUser.getId());
        return newUser;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserStats getUserStats(Long userId) {
        return userStatsRepository.findByUserId(userId).orElse(null);
    }

    public User logInUser(UserPostDTO userPostDTO) {
        User user = userRepository.findByUsername(userPostDTO.getUsername());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String providedHash = hashPassword(userPostDTO.getPassword());
        if (!providedHash.equals(user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String token = UUID.randomUUID().toString();
        user.setToken(token);

        userRepository.save(user);
        userRepository.flush();

        return user;
    }

    // We keep this method to avoid breaking controller routes at compile time.
    public User authenticateUser(String token) {

        if (token == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token missing");
        }

        User user = userRepository.findByToken(token);
    
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }
        return user;
    }

    public void logOutUser(Long id) {
        User user = getUserById(id);
        user.setToken(null);

        userRepository.save(user);
        userRepository.flush();
    }

    public void updateUser(Long id, UserPostDTO userPostDTO) {
        User user = getUserById(id);

        if (userPostDTO.getUsername() != null && !userPostDTO.getUsername().isBlank()
                && !user.getUsername().equals(userPostDTO.getUsername())) {
            checkIfUsernameExists(userPostDTO.getUsername());
            user.setUsername(userPostDTO.getUsername());
        }

        if (userPostDTO.getPassword() != null && !userPostDTO.getPassword().isBlank()) {
            user.setPasswordHash(hashPassword(userPostDTO.getPassword()));
        }

        userRepository.save(user);
        userRepository.flush();
    }

    private void checkIfUsernameExists(String username) {
        User userByUsername = userRepository.findByUsername(username);
        if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The username provided is not unique. Therefore, the user could not be created!");
        }
    }

    private String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not be empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 hashing algorithm unavailable", e);
        }
    }

    public void updatePlayerStats(Game game) {
        // determine winner
        Player winnerPlayer = game.getPlayerOrder().stream()
            .filter(Player::isAlive)
            .findFirst()
            .orElse(null);

        // update stats for all players in the game
        List<UserStats> toSave = new ArrayList<>();
        for (Player p : game.getPlayerOrder()) {
            Long userId = p.getUser().getId();
            UserStats stats = userStatsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("UserStats for user " + userId + " not found. Stats should be created with the user."));
            stats.setGamesPlayed(stats.getGamesPlayed() + 1);
            if (winnerPlayer != null && p.getPlayerId().equals(winnerPlayer.getPlayerId())) {
                stats.setWins(stats.getWins() + 1);
            }
            toSave.add(stats);
        }
        userStatsRepository.saveAll(toSave);
    }
    public List<UserStatsDTO> getLeaderboard() {
    List<UserStats> allStats = userStatsRepository.findAll();

    List<UserStatsDTO> dtos = allStats.stream().map(stats -> {
        UserStatsDTO dto = new UserStatsDTO();
        dto.setUserId(stats.getUser().getId());
        dto.setUsername(stats.getUser().getUsername());
        dto.setGamesPlayed(stats.getGamesPlayed());
        dto.setWins(stats.getWins());

        if (stats.getGamesPlayed() != null && stats.getGamesPlayed() > 0) {
            dto.setWinPercentage((double) stats.getWins() / stats.getGamesPlayed());
        } else {
            dto.setWinPercentage(0.0);
        }
        return dto;
    }).collect(Collectors.toList());

    // Sorts the leaderboard: 1. wins (desc), 2. games played (asc), 3. username (asc)
    dtos.sort((a, b) -> {
        int cmp = b.getWins().compareTo(a.getWins());
        if (cmp != 0) return cmp;
        cmp = a.getGamesPlayed().compareTo(b.getGamesPlayed());
        if (cmp != 0) return cmp;
        return a.getUsername().compareToIgnoreCase(b.getUsername());
    });

    return dtos;
}
}
