package org.hamisi.swoopdserver.notification;

import org.hamisi.swoopdserver.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface MessagingTokenRepository extends JpaRepository<User, UUID> {
    @Query("SELECT u.messagingToken FROM User u where u.userId = :userId")
    String getMessagingByTokenUserId(UUID userId);
}
