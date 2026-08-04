package org.hamisi.swoopdserver.auth.repository;

import org.hamisi.swoopdserver.users.User;
import org.hamisi.swoopdserver.users.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UsersRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    default void addUser(User user) {
        save(user);
    }
    @Query("SELECT u.userId FROM User u WHERE u.email = :email")
    UUID findUserIdByEmail(String email);

    User getUserByUserId(UUID userId);

    User findByEmail(String email);
    @Modifying
    @Query("UPDATE User u " +
            "SET u.messagingToken = :messagingToken WHERE u.userId = :userId")
    void setMessagingToken(String messagingToken, UUID userId);

    @Query("select n from User u join u.fullName n where u.userId=:userId")
    String getFullNameByUserId(UUID userId);

    @Query("select u.userId from User u where u.email = :email")
    UUID getUserIdByEmail(String email);
    @Query("select u.userId from User u")
    List<UUID> getAllUserIds();

    List<User> findAllByOrderByFullNameAsc();

    List<User> findByUserIdInOrderByFullNameAsc(Collection<UUID> userIds);

    @Query("SELECT u FROM User u WHERE u.messagingToken IS NOT NULL AND LENGTH(TRIM(u.messagingToken)) > 0 ORDER BY u.fullName ASC")
    List<User> findUsersWithMessagingTokens();

    @Modifying
    @Query("UPDATE User u SET u.role = :role WHERE u.userId = :userId")
    void updateRole(@Param("userId") UUID userId, @Param("role") Role role);
}
