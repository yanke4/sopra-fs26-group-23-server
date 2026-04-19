package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStartDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyWebSocketDTO;


@Service
@Transactional
public class LobbyService {
    private final LobbyRepository lobbyRepository;
    private final UserService userService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public LobbyService(LobbyRepository lobbyRepository, UserService userService, GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.lobbyRepository = lobbyRepository;
        this.userService = userService;
        this.gameService = gameService;
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
        if (lobby.getJointUsers().isEmpty()) {
            lobby.setStatus(LobbyStatus.CLOSED);
        } else {
            
            User newHost = lobby.getJointUsers().get(0);
            lobby.setHost(newHost);
            
            List<User> remaining = new java.util.ArrayList<>(lobby.getJointUsers());
            remaining.remove(0);
            lobby.setJointUsers(remaining);
           
        }
    } else {
        List<User> remaining = lobby.getJointUsers().stream()
            .filter(u -> !u.getId().equals(userId))
            .collect(java.util.stream.Collectors.toList());
        lobby.setJointUsers(remaining);
    }

    lobby = lobbyRepository.save(lobby);
    lobbyRepository.flush();
    broadcastLobbyUpdate(lobby);
}

    public GameStartDTO startGame(Long lobbyId, Long userId){
        Lobby lobby = getLobbyById(lobbyId);

        if (!lobby.getHost().getId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the host can start the game."
            );
        }

        if (lobby.getJointUsers().size() < 1) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "At least 2 players are required to start the game."
            );
        }

        // Create game BEFORE closing lobby — if this fails, lobby stays OPEN
        Game game = gameService.createGame(lobby);

        lobby.setStatus(LobbyStatus.CLOSED);
        lobby = lobbyRepository.save(lobby);
        lobbyRepository.flush();

        final Long finalLobbyId = lobby.getLobbyId();
        final Long finalGameId = game.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                broadcastGameStarted(finalLobbyId, finalGameId);
            }
        });

        GameStartDTO dto = new GameStartDTO();
        dto.setLobbyId(lobby.getLobbyId());
        dto.setGameId(game.getId());
        return dto;
    }
    
    private void broadcastGameStarted(Long lobbyId, Long gameId) {
        GameStartDTO dto = new GameStartDTO();
        dto.setLobbyId(lobbyId);
        dto.setGameId(gameId);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, dto);
    }
}
