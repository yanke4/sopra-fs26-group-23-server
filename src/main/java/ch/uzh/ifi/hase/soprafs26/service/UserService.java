package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(@Qualifier("userRepository") UserRepository userRepository) {
        this.userRepository = userRepository;
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

        log.debug("Created Information for User: {}", newUser.getId());
        return newUser;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
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

    // Legacy token-based endpoints are no longer backed by token persistence.
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
}
