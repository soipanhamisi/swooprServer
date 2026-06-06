package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.exceptions.IncorrectPasswordException;
import org.hamisi.swoopdserver.auth.exceptions.UserDoesntExistException;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SimpleAuthService {

    private final UsersRepository usersRepository;

    public SimpleAuthService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public void checkEmailPassword(String email, String password){
        if(usersRepository.findByEmail(email).isEmpty()){
            throw new UserDoesntExistException("User doesnt exist");
        }
        if(!Objects.equals(usersRepository.findByEmail(email).get().getPassword(),
                new HashingService().hashPassword(password))){
            throw new IncorrectPasswordException("Incorrect password");
        }
    }
}
