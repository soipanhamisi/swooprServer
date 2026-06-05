package org.hamisi.swoopdserver.auth.repository;

import org.hamisi.swoopdserver.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    default void addUser(User user) {
        save(user);
    }
}
