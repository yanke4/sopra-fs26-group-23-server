package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Region;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegionService {

    private final GameRepository gameRepository;

    public RegionService(GameRepository gamerepository){
        this.gameRepository = gamerepository;
    }

    public int calculateRegionBonus(Long gameId, Player player){
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));

        if (player == null || player.getPlayerId() == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found.");
        }

        if (game.getMap() == null || game.getMap().getRegions() == null){
            return 0;
        }

        int regionBonus = 0; 

        for (Region region : game.getMap().getRegions()){
            if (region.isOwnedBy(player)){
                regionBonus += region.getBonusAmount();
            }
        }
        return regionBonus;

    }  
}