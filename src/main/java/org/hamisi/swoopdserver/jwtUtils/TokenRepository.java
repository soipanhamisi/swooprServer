package org.hamisi.swoopdserver.jwtUtils;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    Optional<Token> findByToken(String token);
    void deleteByToken(String token);
    boolean existsByToken(String token);

    @Modifying
    @Query("DELETE FROM Token t WHERE t.expiresAt < :oneWeekAgo")
    long deleteByExpiresAtBefore(LocalDateTime oneWeekAgo);
}