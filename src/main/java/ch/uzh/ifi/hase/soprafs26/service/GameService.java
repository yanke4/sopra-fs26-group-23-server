package ch.uzh.ifi.hase.soprafs26.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.PlayerColor;
import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Map;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Region;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateDTO;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RegionService regionService;
    private final MissionService missionService;

    public GameService(GameRepository gameRepository, SimpMessagingTemplate messagingTemplate,
                       RegionService regionService, MissionService missionService) {
        this.gameRepository = gameRepository;
        this.messagingTemplate = messagingTemplate;
        this.regionService = regionService;
        this.missionService = missionService;
    }

    private long calculateReinforcements(Long gameId, Player player) {
        int fromRegions = regionService.calculateRegionBonus(gameId, player);
        return 4L + fromRegions;
    }

    public GameStateDTO getGameState(Long gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Game " + gameId + " not found."
            ));
        return convertToGameStateDTO(game);
    }

    public void advancePhase(Long gameId, Long playerId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Game " + gameId + " not found."));

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null || !currentPlayer.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "It's not player " + playerId + "'s turn.");
        }

        GamePhase phase = game.getCurrentPhase();
        switch (phase) {
            case DEPLOY:
                if (currentPlayer.getTroopCount() > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You must deploy all remaining troops before advancing.");
                }
                game.setCurrentPhase(GamePhase.ATTACK);
                break;
            case ATTACK:
                game.setCurrentPhase(GamePhase.FORTIFY);
                break;
            case FORTIFY:
                // Move to next alive player and reset to DEPLOY
                game.setMoveDoneThisTurn(false);
                if (!currentPlayer.isHasAttackedThisTurn()) {
                    currentPlayer.setPeacefulTurnsCompleted(currentPlayer.getPeacefulTurnsCompleted() + 1);
                }
                int nextIndex = game.getCurrentPlayerIndex();
                List<Player> players = game.getPlayerOrder();
                int startIndex = nextIndex;
                do {
                    nextIndex = (nextIndex + 1) % players.size();
                } while (!players.get(nextIndex).isAlive() && nextIndex != startIndex);
                game.setCurrentPlayerIndex(nextIndex);
                // A new turn starts when the play order wraps back to (or past) the previous player's index
                if (nextIndex <= startIndex) {
                    game.setTurnNumber(game.getTurnNumber() + 1);
                }
                game.setCurrentPhase(GamePhase.DEPLOY);
                Player nextPlayer = players.get(nextIndex);
                nextPlayer.setTroopCount(calculateReinforcements(gameId, nextPlayer) + nextPlayer.getPendingMissionBonus());
                nextPlayer.setPendingMissionBonus(0L);
                nextPlayer.setTroopsKilledThisTurn(0L);
                nextPlayer.setHasAttackedThisTurn(false);
                nextPlayer.setTerritoriesConqueredThisTurn(0);
                game.setTurnStartedAtMillis(System.currentTimeMillis());
                break;
        }

        refreshMissions(game);

        gameRepository.save(game);
        gameRepository.flush();
        broadcastGameState(game);
    }

    /**
     * Rotates expired missions and evaluates active ones for every alive player.
     * Called after each state-changing action so completion is visible immediately.
     */
    public void refreshMissions(Game game) {
        for (Player player : game.getPlayerOrder()) {
            if (!player.isAlive()) continue;
            missionService.rotateIfDue(player, game);
            missionService.evaluateAndReward(player, game);
        }
    }

    /**
     * Force-end the current player's turn — called when their turn timer expires.
     * Forfeits unspent reinforcements and advances to the next alive player at DEPLOY.
     */
    public void forceEndTurn(Long gameId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getStatus() != GameStatus.RUNNING) return;

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        Long timedOutPlayerId = currentPlayer.getPlayerId();

        currentPlayer.setTroopCount(0L);
        if (!currentPlayer.isHasAttackedThisTurn()) {
            currentPlayer.setPeacefulTurnsCompleted(currentPlayer.getPeacefulTurnsCompleted() + 1);
        }

        List<Player> players = game.getPlayerOrder();
        int startIndex = game.getCurrentPlayerIndex();
        int nextIndex = startIndex;
        do {
            nextIndex = (nextIndex + 1) % players.size();
        } while (!players.get(nextIndex).isAlive() && nextIndex != startIndex);

        game.setCurrentPlayerIndex(nextIndex);
        if (nextIndex <= startIndex) {
            game.setTurnNumber(game.getTurnNumber() + 1);
        }
        game.setCurrentPhase(GamePhase.DEPLOY);
        game.setMoveDoneThisTurn(false);
        Player nextPlayer = players.get(nextIndex);
        nextPlayer.setTroopCount(calculateReinforcements(gameId, nextPlayer) + nextPlayer.getPendingMissionBonus());
        nextPlayer.setPendingMissionBonus(0L);
        nextPlayer.setTroopsKilledThisTurn(0L);
        nextPlayer.setHasAttackedThisTurn(false);
        nextPlayer.setTerritoriesConqueredThisTurn(0);
        game.setTurnStartedAtMillis(System.currentTimeMillis());

        refreshMissions(game);

        gameRepository.save(game);
        gameRepository.flush();

        GameStateDTO dto = convertToGameStateDTO(game);
        dto.setTimedOutPlayerId(timedOutPlayerId);
        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), dto);
    }

    /**
     * Polls every second for games whose current player's turn has exceeded the
     * configured timer and force-ends them.
     */
    @Scheduled(fixedRate = 1000)
    public void enforceTurnTimers() {
        List<Game> games = gameRepository.findAll();
        long now = System.currentTimeMillis();
        for (Game game : games) {
            if (game.getStatus() != GameStatus.RUNNING) continue;
            Integer timer = game.getTurnTimerSeconds();
            Long startedAt = game.getTurnStartedAtMillis();
            if (timer == null || startedAt == null) continue;
            long deadline = startedAt + timer * 1000L + 500L; // +500ms grace for clock drift
            if (now >= deadline) {
                try {
                    forceEndTurn(game.getId());
                } catch (Exception e) {
                    // Swallow so a single bad game doesn't kill the scheduler.
                }
            }
        }
    }

    public void broadcastGameUpdate(Long gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game " + gameId + " not found."));
        broadcastGameState(game);
    }

    public void broadcastGameUpdate(Long gameId, GameStateDTO.AttackEventDTO lastAttack) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game " + gameId + " not found."));
        GameStateDTO gameStateDTO = convertToGameStateDTO(game);
        gameStateDTO.setLastAttack(lastAttack);
        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), gameStateDTO);
    }

    public void broadcastGameState(Game game) {
        GameStateDTO gameStateDTO = convertToGameStateDTO(game);
        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), gameStateDTO);
    }

    public void broadcastGameState(Game game, GameStateDTO.AttackEventDTO lastAttack) {
        GameStateDTO gameStateDTO = convertToGameStateDTO(game);
        gameStateDTO.setLastAttack(lastAttack);
        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), gameStateDTO);
    }

    private GameStateDTO convertToGameStateDTO(Game game) {
        GameStateDTO gameStateDTO = new GameStateDTO();
        gameStateDTO.setGameId(game.getId());
        gameStateDTO.setStatus(game.getStatus());
        gameStateDTO.setCurrentPlayerIndex(game.getCurrentPlayerIndex());
        gameStateDTO.setCurrentPlayerId(game.getCurrentPlayer() != null ? game.getCurrentPlayer().getPlayerId() : null);
        gameStateDTO.setCurrentPhase(game.getCurrentPhase());
        gameStateDTO.setMoveDoneThisTurn(game.isMoveDoneThisTurn());
        gameStateDTO.setTurnNumber(game.getTurnNumber());
        gameStateDTO.setTurnTimerSeconds(game.getTurnTimerSeconds());
        gameStateDTO.setTurnStartedAtMillis(game.getTurnStartedAtMillis());
        gameStateDTO.setFogOfWarEnabled(game.isFogOfWarEnabled());

        final int currentRound = game.getTurnNumber();
        gameStateDTO.setPlayers(
            game.getPlayerOrder().stream().map(player -> {
                GameStateDTO.PlayerStateDTO playerDTO = new GameStateDTO.PlayerStateDTO();
                playerDTO.setPlayerId(player.getPlayerId());
                playerDTO.setUserId(player.getUser().getId());
                playerDTO.setUsername(player.getUser().getUsername());
                playerDTO.setColor(player.getColor());
                playerDTO.setAlive(player.isAlive());
                playerDTO.setTroopCount(player.getTroopCount());
                if (player.getCurrentMissionType() != null) {
                    playerDTO.setMissionType(player.getCurrentMissionType());
                    playerDTO.setMissionDescription(player.getCurrentMissionType().getDescription());
                    playerDTO.setMissionStatus(player.computeMissionStatus(currentRound));
                    playerDTO.setMissionStartRound(player.getMissionStartRound());
                    playerDTO.setMissionExpiresAtRound(player.getMissionStartRound() + 3);
                    playerDTO.setMissionBonusTroops(player.getCurrentMissionType().getBonusTroops());
                }
                return playerDTO;
            }).collect(Collectors.toList())
        );
        gameStateDTO.setFields(
            game.getMap().getRegions().stream()
                .flatMap(region -> region.getFields().stream())
                .map(field -> {
                    GameStateDTO.FieldStateDTO fieldDTO = new GameStateDTO.FieldStateDTO();
                    fieldDTO.setFieldName(field.getName());
                    fieldDTO.setOwnerPlayerId(field.getOwner() != null ? field.getOwner().getPlayerId() : null);
                    fieldDTO.setTroops(field.getTroops());
                    return fieldDTO;
                }).collect(Collectors.toList())
        );
        return gameStateDTO;
    }

    private Long generateUniqueGameId() {
        Random random = new Random();
        Long id;
        do {
            id = 100000L + (long) (random.nextDouble() * 900000);
        } while (gameRepository.existsById(id));
        return id;
    }

    public Game createGame(Lobby lobby) {
        Game game = new Game();
        game.setId(generateUniqueGameId());
        game.setStatus(GameStatus.RUNNING);
        game.setCurrentPlayerIndex(0);
        game.setCurrentPhase(GamePhase.DEPLOY);
        game.setTurnNumber(1);
        game.setTurnTimerSeconds(lobby.getTurnTimerSeconds());
        game.setFogOfWarEnabled(lobby.isFogOfWarEnabled());
        game.setTurnStartedAtMillis(System.currentTimeMillis() + 5000L);

        List<Player> players = createPlayers(lobby, game);
        game.setPlayerOrder(players);

        Map map = createMap();
        game.setMap(map);

        assignTerritories(map, players);

        game = gameRepository.save(game);
        gameRepository.flush();

        for (Player player : game.getPlayerOrder()) {
            missionService.assignInitialMission(player, game);
        }

        Player firstPlayer = game.getPlayerOrder().get(0);
        firstPlayer.setTroopCount(calculateReinforcements(game.getId(), firstPlayer));

        gameRepository.flush();
        broadcastGameState(game);
        return game;
    }

    private List<Player> createPlayers(Lobby lobby, Game game) {
        List<User> allUsers = new ArrayList<>();
        allUsers.add(lobby.getHost());
        allUsers.addAll(lobby.getJointUsers());
        Collections.shuffle(allUsers);

        java.util.Map<Long, PlayerColor> colorPrefs = lobby.getColorPreferences() != null
                ? lobby.getColorPreferences()
                : new HashMap<>();

        java.util.Set<PlayerColor> takenColors = new java.util.HashSet<>(colorPrefs.values());

        java.util.List<PlayerColor> fallbackColors = Arrays.stream(PlayerColor.values())
          .filter(c -> !takenColors.contains(c))
          .collect(java.util.stream.Collectors.toList());
        int fallbackIdx = 0;

        List<Player> players = new ArrayList<>();
        for (User user : allUsers) {
            Player player = new Player();
            player.setUser(user);
            player.setGame(game);
            player.setLobby(lobby);

            PlayerColor color = colorPrefs.containsKey(user.getId())
                    ? colorPrefs.get(user.getId())
                    : fallbackColors.get(fallbackIdx++);

            player.setColor(color);
            player.setAlive(true);
            player.setTroopCount(0L);
            players.add(player);
        }
        return players;
    }

    private Map createMap() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/map.json");
            JsonNode root = objectMapper.readTree(is);

            Map map = new Map();
            HashMap<String, Field> fieldsByName = new HashMap<>();
            List<Region> regions = new ArrayList<>();

            for (JsonNode regionNode : root.get("regions")) {
                Region region = new Region();
                region.setName(regionNode.get("name").asText());
                region.setBonusAmount(regionNode.get("bonus").asInt());
                region.setMap(map);

                List<Field> fields = new ArrayList<>();
                for (JsonNode fieldName : regionNode.get("fields")) {
                    Field field = new Field();
                    field.setName(fieldName.asText());
                    field.setTroops(0L);
                    field.setRegion(region);
                    fields.add(field);
                    fieldsByName.put(fieldName.asText(), field);
                }
                region.setFields(fields);
                regions.add(region);
            }

            // set neighbours
            JsonNode neighboursNode = root.get("neighbours");
            neighboursNode.fieldNames().forEachRemaining(fieldName -> {
                Field field = fieldsByName.get(fieldName);
                List<Field> neighbours = new ArrayList<>();
                for (JsonNode neighbourName : neighboursNode.get(fieldName)) {
                    neighbours.add(fieldsByName.get(neighbourName.asText()));
                }
                field.setNeighbours(neighbours);
            });

            map.setRegions(regions);
            return map;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load map from map.json", e);
        }
    }

    private void assignTerritories(Map map, List<Player> players) {
        List<Field> allFields = new ArrayList<>();
        for (Region region : map.getRegions()) {
            allFields.addAll(region.getFields());
        }

        Collections.shuffle(allFields);

        List<Field> available = new ArrayList<>(allFields);
        Set<String> claimedNames = new HashSet<>();
        for (Player player : players) {
            Field field1 = findSpawnPair(available, claimedNames, true);
            Field field2 = null;

            if (field1 != null) {
                field2 = findSpawnPartner(field1, available, claimedNames, true);
            }

            if (field1 == null || field2 == null) {
                field1 = findSpawnPair(available, claimedNames, false);
                if (field1 != null) {
                    field2 = findSpawnPartner(field1, available, claimedNames, false);
                }
            }

            if (field1 == null || field2 == null) {
                field1 = available.get(0);
                field2 = available.get(1);
            }

            available.remove(field1);
            available.remove(field2);
            claimedNames.add(field1.getName());
            claimedNames.add(field2.getName());

            field1.setOwner(player);
            field2.setOwner(player);
            field1.setTroops(2L);
            field2.setTroops(3L);
        }

        for (Field field : available) {
            field.setOwner(null);
            field.setTroops(1L);
        }
    }

    private Field findSpawnPair(List<Field> available, Set<String> claimedNames,
                                boolean avoidClaimedNeighbours) {
        for (Field candidate : available) {
            if (avoidClaimedNeighbours && isAdjacentToAny(candidate, claimedNames)) {
                continue;
            }
            if (findSpawnPartner(candidate, available, claimedNames, avoidClaimedNeighbours) != null) {
                return candidate;
            }
        }
        return null;
    }

    private Field findSpawnPartner(Field first, List<Field> available, Set<String> claimedNames,
                                   boolean avoidClaimedNeighbours) {
        for (Field candidate : available) {
            if (candidate == first) continue;
            if (avoidClaimedNeighbours && isAdjacentToAny(candidate, claimedNames)) {
                continue;
            }
            boolean adjacentToFirst = first.getNeighbours() != null
                && first.getNeighbours().stream()
                    .anyMatch(n -> n.getName().equals(candidate.getName()));
            if (!adjacentToFirst) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isAdjacentToAny(Field field, Set<String> names) {
        if (names.isEmpty() || field.getNeighbours() == null) return false;
        return field.getNeighbours().stream().anyMatch(n -> names.contains(n.getName()));
    }

    public void assignReinforcementsToPlayer(Long gameId, Player player) {
        player.setTroopCount(calculateReinforcements(gameId, player));
    }
}