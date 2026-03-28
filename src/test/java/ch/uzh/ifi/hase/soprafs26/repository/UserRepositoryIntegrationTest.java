package ch.uzh.ifi.hase.soprafs26.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.entity.User;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class UserRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void findByUsername_success() {
        User user = new User();
        user.setUsername("firstname@lastname");
        user.setPasswordHash("hashed-password");
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        entityManager.persist(user);
        entityManager.flush();

        User found = userRepository.findByUsername(user.getUsername());

        assertNotNull(found.getId());
        assertEquals(found.getUsername(), user.getUsername());
        assertEquals(found.getPasswordHash(), user.getPasswordHash());
        assertEquals(found.getCreatedAt(), user.getCreatedAt());
    }
}
