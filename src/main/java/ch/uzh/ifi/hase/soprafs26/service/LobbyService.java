package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    public Lobby getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Lobby " + lobbyId + " not found."
            ));
}

    private void broadcastLobbyUpdate(Lobby lobby) {
        LobbyWebSocketDTO dto = new LobbyWebSocketDTO();
        dto.setLobbyId(lobby.getLobbyId());
        dto.setStatus(lobby.getStatus());
        dto.setJoinCode(lobby.getJoinCode());
        dto.setHostId(lobby.getHost().getId());
        dto.setJointUserIds(
            lobby.getJointUsers().stream()
                .map(User::getId)
                .toList()
        );
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getLobbyId(), dto);
}

    public Lobby createLobby(Long hostId) {
        User host = userService.getUserById(hostId);

        Lobby newLobby = new Lobby();
        newLobby.setHost(host);
        newLobby.setStatus(LobbyStatus.OPEN);
        newLobby.setJoinCode(generateUniqueJoinCode());

        newLobby = lobbyRepository.save(newLobby);
        lobbyRepository.flush();
        broadcastLobbyUpdate(newLobby);

        return newLobby;
    }

    public Lobby joinLobby(Long joinCode, Long userId) {

        Lobby lobby = lobbyRepository.findByJoinCode(joinCode);
        if (lobby == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No lobby found with code " + joinCode + "."
            );
        }

        if (lobby.getStatus() != LobbyStatus.OPEN) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Lobby is no longer open."
            );
        }

        final int MAX_PLAYERS = 4;
        if (lobby.getJointUsers().size() >= MAX_PLAYERS - 1) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Lobby is full."
            );
        }

        User user = userService.getUserById(userId);

        boolean isHost = lobby.getHost().getId().equals(userId);
        boolean alreadyJoined = lobby.getJointUsers()
            .stream()
            .anyMatch(u -> u.getId().equals(userId));

        if (!isHost && !alreadyJoined) {
            lobby.getJointUsers().add(user);
            lobby = lobbyRepository.save(lobby);
            lobbyRepository.flush();
        }
        broadcastLobbyUpdate(lobby);
        return lobby;
    }

    private Long generateUniqueJoinCode() {
        Long joinCode;
        do {
            joinCode = (long) (Math.random() * 900000) + 100000; // Generate a random 6-digit number
        } while (lobbyRepository.findByJoinCode(joinCode) != null); // Make sure its unique
        return joinCode;
    }

    public void leaveLobby(Long lobbyId, Long userId) {
        Lobby lobby = getLobbyById(lobbyId);

        boolean isHost = lobby.getHost().getId().equals(userId);
        if (isHost) {
            lobby.setStatus(LobbyStatus.CLOSED);
            lobby.getJointUsers().clear();
        } else {
            lobby.getJointUsers().removeIf(u -> u.getId().equals(userId));
        }

        lobby = lobbyRepository.save(lobby);
        lobbyRepository.flush();
        broadcastLobbyUpdate(lobby);
    }
}
