package org.hamisi.swoopdserver.auth.repository;

import org.hamisi.swoopdserver.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
