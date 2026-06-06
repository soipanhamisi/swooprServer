package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.exceptions.IncorrectPasswordException;
import org.hamisi.swoopdserver.auth.exceptions.UserDoesntExistException;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.jwtUtils.TokenManagementService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SimpleAuthService {

    private final UsersRepository usersRepository;
    private final TokenManagementService tokenManagementService;

    public SimpleAuthService(UsersRepository usersRepository, TokenManagementService tokenManagementService) {
        this.usersRepository = usersRepository;
        this.tokenManagementService = tokenManagementService;
    }

    public String checkEmailPassword(String email, String password){
        if(usersRepository.findByEmail(email).isEmpty()){
            throw new UserDoesntExistException("User doesnt exist");
        }
        if(!Objects.equals(usersRepository.findByEmail(email).get().getPassword(),
                new HashingService().hashPassword(password))){
            throw new IncorrectPasswordException("Incorrect password");
        }

        return tokenManagementService.createToken(
                usersRepository.findUserIdByEmail(email),
                usersRepository.findFullNameByEmail(email)
        );
    }

}
