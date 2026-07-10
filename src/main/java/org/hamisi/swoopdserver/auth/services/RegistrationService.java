package org.hamisi.swoopdserver.auth.services;

import jakarta.transaction.Transactional;
import org.hamisi.swoopdserver.auth.dtos.UserDTO;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.auth.exceptions.UserExistsException;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.users.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegistrationService {

    private final UsersRepository usersRepository;
    private final TokenManagementService tokenManagementService;


    public RegistrationService(UsersRepository usersRepository, TokenManagementService tokenManagementService) {
        this.usersRepository = usersRepository;
        this.tokenManagementService = tokenManagementService;
    }
    @Transactional
    public String registerUser(UserDTO user){
        if (usersRepository.existsByEmail(user.getEmail())){
            throw new UserExistsException("User already exists");
        }
        User userEntity = new User();
        userEntity.setFullName(user.getFullName());
        userEntity.setEmail(user.getEmail());
        userEntity.setRole(user.getRole());
        usersRepository.addUser(userEntity);

        UUID userId = usersRepository.findUserIdByEmail(userEntity.getEmail());
        setMessagingToken(user.getMessagingToken(), userId);

        return tokenManagementService.createToken(userId, userEntity.getEmail());
    }
    public void setMessagingToken(String messagingToken, UUID userId){
        usersRepository.setMessagingToken(messagingToken, userId);
    }

}
