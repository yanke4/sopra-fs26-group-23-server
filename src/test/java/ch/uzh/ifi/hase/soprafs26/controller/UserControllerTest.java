package ch.uzh.ifi.hase.soprafs26.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserStatsDTO;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("firstname@lastname");
        user.setPasswordHash("hash");
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        List<User> allUsers = Collections.singletonList(user);

        given(userService.getUsers()).willReturn(allUsers);

        MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(user.getId().intValue())))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())))
                .andExpect(jsonPath("$[0].createdAt", is(user.getCreatedAt().toString())));
    }

    @Test
    public void createUser_validInput_userCreated() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUsername");
        user.setPasswordHash("hash");
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("plainPassword");

        given(userService.createUser(Mockito.any())).willReturn(user);

        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.createdAt", is(user.getCreatedAt().toString())));
    }

	@Test
	public void createUser_duplicateUsername_returnsConflict() throws Exception{

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("TestLinusUsername");

		given(userService.createUser(Mockito.any()))
					.willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists"));

		MockHttpServletRequestBuilder postRequest = post("/users")
			.contentType(MediaType.APPLICATION_JSON)
			.content(asJsonString(userPostDTO));
	
		mockMvc.perform(postRequest)
		.andExpect(status().isConflict());

	}

	@Test
	public void getUser_validId_returnUser() throws Exception{
		
		User user = new User(); 
		user.setId(1L);
		user.setUsername("TestLinus");

		given(userService.getUserById(1L)).willReturn(user);

		mockMvc.perform(get("/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.username", is("TestLinus")));
	}

@Test
	public void getUser_invalidId_userNotFound() throws Exception{
		
		given(userService.getUserById(99L))
		.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with userId doesn't exist!"));

		mockMvc.perform(get("/users/99"))
		.andExpect(status().isNotFound());

	}
	@Test
	public void updateUserProfile_validPassword() throws Exception{
		long id = 1L;
		User user = new User();
		user.setId(id);
		user.setUsername("updateUsernameLinus");
		user.setToken("1");
		user.setPasswordHash("hashedPassword");

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setPassword("updatePasswordLinus");

		given(userService.authenticateUser(anyString())).willReturn(user);
		doNothing().when(userService).updateUser(eq(id), any(UserPostDTO.class));

		MockHttpServletRequestBuilder putRequest = put("/users/1")
		.header("token", "1")
		.contentType(MediaType.APPLICATION_JSON)
		.content(asJsonString(userPostDTO));
	

		mockMvc.perform(putRequest)
		.andExpect(status().isNoContent());

        // Verify that the updateUser method was called with the correct parameters
        verify(userService).updateUser(eq(1L), any(UserPostDTO.class));
	}

	@Test 
	public void updateUserProfile_wrongPassword() throws Exception{
		User user = new User();
		user.setId(1L);
		user.setUsername("updateUsernameLinus");
		user.setToken("1");
		user.setPasswordHash("hashedPassword");

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setPassword("updateWrongPasswordLinus");

		given(userService.authenticateUser(anyString())).willReturn(user);

		willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password is wrong!"))
      	.given(userService).updateUser(eq(1L), any(UserPostDTO.class));

		MockHttpServletRequestBuilder putRequest = put("/users/1")
		.header("token", "1")
		.contentType(MediaType.APPLICATION_JSON)
		.content(asJsonString(userPostDTO));

		mockMvc.perform(putRequest)
		.andExpect(status().isUnauthorized());

	}

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }

	@Test
    public void loginUser_validCredentials_returns200WithToken() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setToken("secret-token");
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
 
        given(userService.logInUser(Mockito.any())).willReturn(user);
 
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("alice");
        dto.setPassword("correct");
 
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",       is(1)))
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.token",    is("secret-token")));
    }
 
    @Test
    public void loginUser_wrongCredentials_returns401() throws Exception {
        given(userService.logInUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials."));
 
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("alice");
        dto.setPassword("wrong");
 
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isUnauthorized());
    }
 
    @Test
    public void loginUser_unknownUser_returns404() throws Exception {
        given(userService.logInUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
 
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("nobody");
        dto.setPassword("pass");
 
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isNotFound());
    }
 
    // -----------------------------------------------------------------------
    // POST /auth/logout
    // -----------------------------------------------------------------------
 
    @Test
    public void logoutUser_validToken_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setToken("valid-token");
 
        given(userService.authenticateUser("valid-token")).willReturn(user);
        doNothing().when(userService).logOutUser(1L);
 
        mockMvc.perform(post("/auth/logout")
                        .header("token", "valid-token"))
                .andExpect(status().isOk());
    }
 
    @Test
    public void logoutUser_invalidToken_returns401() throws Exception {
        given(userService.authenticateUser("bad-token"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token."));
 
        mockMvc.perform(post("/auth/logout")
                        .header("token", "bad-token"))
                .andExpect(status().isUnauthorized());
    }
 
    // -----------------------------------------------------------------------
    // GET /leaderboard
    // -----------------------------------------------------------------------
 
    @Test
    public void getLeaderboard_multipleEntries_returnsSortedList() throws Exception {
        UserStatsDTO first  = buildStatsDTO(1L, "alice", 10L, 15L, 0.667);
        UserStatsDTO second = buildStatsDTO(2L, "bob",    5L, 12L, 0.417);
 
        given(userService.getLeaderboard()).willReturn(List.of(first, second));
 
        mockMvc.perform(get("/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",              hasSize(2)))
                .andExpect(jsonPath("$[0].userId",    is(1)))
                .andExpect(jsonPath("$[0].username",  is("alice")))
                .andExpect(jsonPath("$[0].wins",      is(10)))
                .andExpect(jsonPath("$[1].userId",    is(2)));
    }
 
    @Test
    public void getLeaderboard_noPlayers_returnsEmptyArray() throws Exception {
        given(userService.getLeaderboard()).willReturn(List.of());
 
        mockMvc.perform(get("/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
 
    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------
 
    private UserStatsDTO buildStatsDTO(Long userId, String username,
                                       Long wins, Long games, Double pct) {
        UserStatsDTO dto = new UserStatsDTO();
        dto.setUserId(userId);
        dto.setUsername(username);
        dto.setWins(wins);
        dto.setGamesPlayed(games);
        dto.setWinPercentage(pct);
        return dto;
    }
}
