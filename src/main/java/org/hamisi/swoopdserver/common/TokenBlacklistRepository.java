package org.hamisi.swoopdserver.common;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenBlacklistRepository extends JpaRepository<Token, Integer> {
    boolean existsTokensByToken(String token);
}
