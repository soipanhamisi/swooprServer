package org.hamisi.swoopdserver.auth.repository;

import org.hamisi.swoopdserver.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UsersRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    default void addUser(User user) {
        save(user);
    }
    @Query("SELECT u.userId FROM User u WHERE u.email = :email")
    UUID findUserIdByEmail(String email);
    @Query("SELECT u.fullName FROM User u WHERE u.email = :email")
    String findFullNameByEmail(String email);

    User getUserByUserId(UUID userId);

    @Modifying
    @Query("UPDATE User u " +
            "SET u.messagingToken = :messagingToken WHERE u.userId = :userId")
    void setMessagingToken(String messagingToken, UUID userId);

    @Query("select n from User u join u.fullName n where u.userId=:userId")
    String getFullNameByUserId(UUID userId);
}
