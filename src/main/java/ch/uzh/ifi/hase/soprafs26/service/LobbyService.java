package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyWebSocketDTO;


@Service
@Transactional
public class LobbyService {
    private final LobbyRepository lobbyRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public LobbyService(LobbyRepository lobbyRepository, UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.lobbyRepository = lobbyRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    public Lobby createLobby(Long hostId) {
        User host = userService.getUserById(hostId);

        Lobby newLobby = new Lobby();
        newLobby.setHost(host);
        newLobby.setStatus(LobbyStatus.OPEN);
        newLobby.setJoinCode(generateUniqueJoinCode());

        newLobby = lobbyRepository.save(newLobby);
        lobbyRepository.flush();

        LobbyWebSocketDTO dto = new LobbyWebSocketDTO();
        dto.setLobbyId(newLobby.getLobbyId());
        dto.setStatus(newLobby.getStatus());
        dto.setJoinCode(newLobby.getJoinCode());
        dto.setHostId(newLobby.getHost().getId());
        dto.setJointUserIds(
            newLobby.getJointUsers().stream()
                .map(User::getId)
                .toList()
                            );

    messagingTemplate.convertAndSend("/topic/lobby/" + newLobby.getLobbyId(), dto);

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
