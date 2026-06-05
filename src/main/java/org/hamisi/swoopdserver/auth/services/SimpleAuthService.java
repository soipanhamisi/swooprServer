package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SimpleAuthService {

    private final UsersRepository usersRepository;

    public SimpleAuthService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public boolean checkEmailPassword(String email, String password){
        return usersRepository.findByEmail(email)
                .map(user -> Objects.equals(user.getPassword(), password))
                .orElse(false);
    }


}
