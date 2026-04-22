package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import java.security.MessageDigest;


import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;


import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUsername");
        testUser.setPasswordHash("plainPassword");

        Mockito.when(userRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private String hash(String raw) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}

    @Test
    public void createUser_validInputs_success() {
        
        Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(null);
        User createdUser = userService.createUser(testUser);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(userRepository, Mockito.times(1)).flush(); //we want to make sure, that flush is called

        assertEquals(testUser.getId(), createdUser.getId());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertNotEquals("plainPassword", createdUser.getPasswordHash());
        assertNotNull(createdUser.getCreatedAt());
    }

    @Test
    public void createUser_duplicateUsername_throwsException() {
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(new User());

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));

        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any()); // we call Mockito.never to make sure, that save was never called, since it should throw an exception
    }

    //To check that it throws an exception when no username is put in. 
    @Test
    public void createUser_missingUsername_throwsException(){
        testUser.setUsername(null);

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));

        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test 
    public void createUser_missingPassword_throwsException(){
        testUser.setPasswordHash(null);

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    
        Mockito.verify(userRepository, Mockito.any()).save(Mockito.any());
    }

    @Test
    public void getUserById_validInputs_success(){

        Mockito.when(userRepository.findById(1L))
        .thenReturn(Optional.of(testUser));

        User foundUser = userService.getUserById(testUser.getId());

        assertEquals(testUser.getUsername(), foundUser.getUsername());
    }

    @Test
    public void getUserById_invalidInputs_throwsException(){
        
        Mockito.when(userRepository.findById(2L)) 
        .thenReturn(Optional.empty()); // optional is used to indicate that a value might be absent. This forces explicit handling of missing data and avoids null pointer exceptions. 
                                       // Pretend the DB didnt find a user with the given id, so it returns an empty optional.
        assertThrows(ResponseStatusException.class, () -> userService.getUserById(2L));
    }

    @Test 
    public void logInUser_validInput_success(){

        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("testUsername");
        dto.setPassword("plainPassword");

        testUser.setPasswordHash(hash("plainPassword"));

        Mockito.when(userRepository.findByUsername("testUsername"))
        .thenReturn(testUser);

        User loggedInUser = userService.logInUser(dto);

        assertEquals(testUser.getUsername(), loggedInUser.getUsername());
        assertNotNull(loggedInUser.getToken());

        Mockito.verify(userRepository).save(testUser);
        Mockito.verify(userRepository).flush();
    }

    @Test 
    public void logInUser_userNotFound_throwsException(){

        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("unknownUser");
        dto.setPassword("plainPassword");

        Mockito.when(userRepository.findByUsername("unknownUser"))
        .thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> userService.logInUser(dto));
        
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void logInUser_wrongPassword_throwsException(){

        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("testUsername");
        dto.setPassword("plainPassword");

        testUser.setPasswordHash("wrongPassword");

        Mockito.when(userRepository.findByUsername("testUsername"))
        .thenReturn(testUser);

        assertThrows(ResponseStatusException.class, () -> userService.logInUser(dto));

        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());


    }

    @Test 
    public void authenticateUser_validToken_success(){

        testUser.setToken("validToken");

        Mockito.when(userRepository.findByToken("validToken"))
        .thenReturn(testUser);

        User authenticatedUser = userRepository.findByToken("validToken");

        assertEquals(testUser.getUsername(), authenticatedUser.getUsername());
    }

}
