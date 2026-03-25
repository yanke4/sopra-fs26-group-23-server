package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;


@Service
@Transactional
public class LobbyService {
    private final LobbyRepository lobbyRepository;
    private final UserService userService;

    public LobbyService(LobbyRepository lobbyRepository, UserService userService) {
        this.lobbyRepository = lobbyRepository;
        this.userService = userService;
    }

    public Lobby createLobby(Long hostId) {
        User host = userService.getUserById(hostId);

        Lobby newLobby = new Lobby();
        newLobby.setHost(host);
        newLobby.setStatus(LobbyStatus.OPEN);
        newLobby.setJoinCode(generateUniqueJoinCode());

        newLobby = lobbyRepository.save(newLobby);
        lobbyRepository.flush();

        return newLobby;
    }

    private Long generateUniqueJoinCode() {
        Long joinCode;
        do {
            joinCode = (long) (Math.random() * 900000) + 100000; // Generate a random 6-digit number
        } while (lobbyRepository.findByJoinCode(joinCode) != null); // Make sure its unique
        return joinCode;
    }

}
