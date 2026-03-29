package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.UserDTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGetDTO> getAllUsers() {
		// fetch all users in the internal representation
		List<User> users = userService.getUsers();
		List<UserGetDTO> userGetDTOs = new ArrayList<>();

		// convert each user to the API representation
		for (User user : users) {
			UserGetDTO userGetDTO = UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
			userGetDTO.setToken(null);
			userGetDTOs.add(userGetDTO);
		}
		return userGetDTOs;
	}

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
		User userInput = UserDTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
		User createdUser = userService.createUser(userInput);
		UserGetDTO userGetDTO = UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
		userGetDTO.setToken(null);

		return userGetDTO;
	}

	@PostMapping("/auth/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO){
		User loginUser = userService.logInUser(userPostDTO);
		return UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(loginUser);
	}

	@GetMapping("/users/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO getUserWithId(@PathVariable("id") Long id){
		User userWithId = userService.getUserById(id);
		UserGetDTO userGetDTO = UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(userWithId);
		userGetDTO.setToken(null);
		return userGetDTO;
	}

	@PostMapping("/auth/logout")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void userLogout(@RequestHeader("token") String token){
		User user = userService.authenticateUser(token);

		userService.logOutUser(user.getId());
	}

	@PutMapping("/users/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ResponseBody
	public void updateUser(@PathVariable("id") Long id, @RequestBody UserPostDTO userPostDTO, @RequestHeader("token") String token){
		User user = userService.authenticateUser(token);

		if(!user.getId().equals(id)){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You're not permitted to update another user");
		}

		userService.updateUser(id, userPostDTO);
	}

}
