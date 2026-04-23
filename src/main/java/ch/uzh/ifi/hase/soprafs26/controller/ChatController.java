package ch.uzh.ifi.hase.soprafs26.controller;

import com.pusher.rest.Pusher;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {
    
    private final Pusher pusher;

    public ChatController(
        @Value("${pusher.appId}") String appId,
        @Value("${pusher.key}") String key,
        @Value("${pusher.secret}") String secret,
        @Value("${pusher.cluster}") String cluster) {

            this.pusher = new Pusher(appId, key, secret);
            this.pusher.setCluster(cluster);
            this.pusher.setEncrypted(true);
            }

    @PostMapping("/message")
    public ResponseEntity<Void> sendMessage(@RequestBody ChatMessageDTO chatMessageDTO) {
        System.out.println("playerId in payload: " + String.valueOf(chatMessageDTO.getPlayerId()));
        Map<String, Object> payload = new HashMap();
        payload.put("playerId", String.valueOf(chatMessageDTO.getPlayerId()));
        payload.put("username", chatMessageDTO.getUsername());
        payload.put("color", chatMessageDTO.getColor());
        payload.put("gameId", chatMessageDTO.getGameId());
        payload.put("message", chatMessageDTO.getMessage());
        payload.put("timestamp", chatMessageDTO.getTimestamp());

        pusher.trigger("presence-game-" + chatMessageDTO.getGameId(), "new-message", payload);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth")
    public ResponseEntity<String> authenticateChannel(@RequestParam("socket_id") String socketId, @RequestParam("channel_name") String channelName, @RequestHeader("x-user-id") Long userId, @RequestHeader("x-user-name") String userName, @RequestHeader("x-user-color") String color) {
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("name", userName);
        userInfo.put("color", color);

        String authResponse = pusher.authenticate(socketId, channelName, 
            new com.pusher.rest.data.PresenceUser(userId, userInfo));


        return ResponseEntity.ok(authResponse);
    }
}
