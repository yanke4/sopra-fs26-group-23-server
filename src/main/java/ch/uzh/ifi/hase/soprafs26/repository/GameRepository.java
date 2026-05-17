package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.Game;

@Repository("gameRepository")
public interface GameRepository extends JpaRepository<Game, Long> {

    @EntityGraph(attributePaths = {
        "playerOrder",
        "playerOrder.user",
        "map",
        "map.regions",
        "map.regions.fields",
        "map.regions.fields.owner",
        "map.regions.fields.neighbours"
    })
    Optional<Game> findWithFullGraphById(Long id);
}