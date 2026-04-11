package ch.uzh.ifi.hase.soprafs26.service;
import ch.uzh.ifi.hase.soprafs26.entity.Field;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Region;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
@Service
public class FieldService {
    public final GameRepository gameRepository;

    public FieldService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Field getFieldByName(String fieldName, Long gameId){
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));

        if (fieldName == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found.");
        }

        if (game.getMap() == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Game map not found.");
        }

        for (Region region : game.getMap().getRegions()){
            if (region.getFields() != null){
                for (Field field : region.getFields()){
                    if(fieldName.equals(field.getName())){
                        return field; 
                    }
                }
            }
            
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found.");
    }

    public int countTerritoriesOwnedByPlayer(Long gameId, Player player){
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));
        
        if (player == null || player.getPlayerId() == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found.");
        }

        if (game.getMap() == null ||game.getMap().getRegions() == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Game map not found.");
        }

        int territoriesOwned = 0;

        for (Region region : game.getMap().getRegions()){
            if(region.getFields() == null){
                continue; 
            }
            for (Field field : region.getFields()){
                if(field.getOwner() != null 
                && field.getOwner().getPlayerId() != null 
                && field.getOwner().getPlayerId().equals(player.getPlayerId())){
                territoriesOwned++;
                }
            }
            
        }
        return territoriesOwned;
    }
    
    public void addUnits(String fieldName, Long troops, Long gameId){
        if (troops == null || troops <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Troops must be a positive number");
        }
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        Field field = getFieldByName(fieldName, gameId);
        
        field.addTroops(troops);
        
        gameRepository.save(game);
    }

    public void removeUnits(String fieldName, Long troops, Long gameId){
        if (troops == null || troops <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Troops must be a positive number");
        }
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        Field field = getFieldByName(fieldName, gameId);
        if (field.getTroops() < troops){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough troops on the field");
        }
        field.removeTroops(troops);
        gameRepository.save(game);
    }
}
