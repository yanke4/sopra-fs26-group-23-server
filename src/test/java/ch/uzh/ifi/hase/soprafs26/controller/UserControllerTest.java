package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
