package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;


@Repository("lobbyRepository")
public interface LobbyRepository extends JpaRepository<Lobby, Long> {
    Lobby findByJoinCode(Long joinCode);
}
