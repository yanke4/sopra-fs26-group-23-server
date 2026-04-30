package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.UserStats;

@Repository("userStatsRepository")
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
	Optional<UserStats> findByUserId(Long userId);
}