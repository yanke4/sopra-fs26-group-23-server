package ch.uzh.ifi.hase.soprafs26.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    public GameService(GameRepository gameRepository, SimpMessagingTemplate messagingTemplate) {
        this.gameRepository = gameRepository;
        this.messagingTemplate = messagingTemplate;
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
                game.setCurrentPhase(GamePhase.ATTACK);
                break;
            case ATTACK:
                game.setCurrentPhase(GamePhase.FORTIFY);
                break;
            case FORTIFY:
                // Move to next alive player and reset to DEPLOY
                game.setMoveDoneThisTurn(false);
                int nextIndex = game.getCurrentPlayerIndex();
                List<Player> players = game.getPlayerOrder();
                do {
                    nextIndex = (nextIndex + 1) % players.size();
                } while (!players.get(nextIndex).isAlive() && nextIndex != game.getCurrentPlayerIndex());
                game.setCurrentPlayerIndex(nextIndex);
                game.setCurrentPhase(GamePhase.DEPLOY);
                break;
        }

        gameRepository.save(game);
        gameRepository.flush();
        broadcastGameState(game);
    }

    //update game state broadcaster for turn actions
    public void broadcastGameUpdate(Long gameId){
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Game " + gameId + " not found."
            ));
        broadcastGameState(game);
    }

    public void broadcastGameState(Game game) {
        GameStateDTO gameStateDTO = convertToGameStateDTO(game);
        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), gameStateDTO);
        }

    //helper method to convert Game entity to GameStateDTO
    private GameStateDTO convertToGameStateDTO(Game game) {
        GameStateDTO gameStateDTO = new GameStateDTO();
        gameStateDTO.setGameId(game.getId());
        gameStateDTO.setStatus(game.getStatus());
        gameStateDTO.setCurrentPlayerIndex(game.getCurrentPlayerIndex());
        gameStateDTO.setCurrentPlayerId(game.getCurrentPlayer() != null ? game.getCurrentPlayer().getPlayerId() : null);
        gameStateDTO.setCurrentPhase(game.getCurrentPhase());
        gameStateDTO.setMoveDoneThisTurn(game.isMoveDoneThisTurn());

        gameStateDTO.setPlayers(
            game.getPlayerOrder().stream().map(player -> {
                GameStateDTO.PlayerStateDTO playerDTO = new GameStateDTO.PlayerStateDTO();
                playerDTO.setPlayerId(player.getPlayerId());
                playerDTO.setUserId(player.getUser().getId());
                playerDTO.setUsername(player.getUser().getUsername());
                playerDTO.setColor(player.getColor());
                playerDTO.setAlive(player.isAlive());
                playerDTO.setTroopCount(player.getTroopCount());
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

        List<Player> players = createPlayers(lobby, game);
        game.setPlayerOrder(players);

        Map map = createMap();
        game.setMap(map);

        assignTerritories(map, players);

        

        game = gameRepository.save(game);
        gameRepository.flush();

        broadcastGameState(game);
        return game;
    }

    private List<Player> createPlayers(Lobby lobby, Game game) {
        // collect all users, host and players
        List<User> allUsers = new ArrayList<>();
        allUsers.add(lobby.getHost());
        allUsers.addAll(lobby.getJointUsers());

        // shuffle to randomize player order
        Collections.shuffle(allUsers);

        PlayerColor[] colors = PlayerColor.values();
        List<Player> players = new ArrayList<>();

        for (int i = 0; i < allUsers.size(); i++) {
            Player player = new Player();
            player.setUser(allUsers.get(i));
            player.setGame(game);
            player.setLobby(lobby);
            player.setColor(colors[i]);
            player.setAlive(true);
            player.setTroopCount(0L); //inital troop count can be changed later
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

            // create regions and fields
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

        // each player gets 2 non-adjacent territories with 2 and 3 troops
        List<Field> available = new ArrayList<>(allFields);
        for (Player player : players) {
            Field field1 = null;
            Field field2 = null;

            for (int i = 0; i < available.size() && field1 == null; i++) {
                Field candidate1 = available.get(i);
                for (int j = i + 1; j < available.size(); j++) {
                    Field candidate2 = available.get(j);
                    boolean areNeighbours = candidate1.getNeighbours() != null
                        && candidate1.getNeighbours().stream()
                            .anyMatch(n -> n.getName().equals(candidate2.getName()));
                    if (!areNeighbours) {
                        field1 = candidate1;
                        field2 = candidate2;
                        break;
                    }
                }
            }

            if (field1 == null || field2 == null) {
                // fallback: take first two available if no non-adjacent pair found
                field1 = available.get(0);
                field2 = available.get(1);
            }

            available.remove(field1);
            available.remove(field2);

            field1.setOwner(player);
            field2.setOwner(player);
            field1.setTroops(2L);
            field2.setTroops(3L);
        }

        // remaining fields are neutral with 1 troop each
        for (Field field : available) {
            field.setOwner(null);
            field.setTroops(1L);
        }
    }
}
