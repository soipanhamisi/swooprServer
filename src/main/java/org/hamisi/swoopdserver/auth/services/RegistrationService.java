package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.dtos.UserDTO;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.auth.exceptions.UserExistsException;
import org.hamisi.swoopdserver.jwtUtils.TokenManagementService;
import org.hamisi.swoopdserver.users.User;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final UsersRepository usersRepository;
    private final TokenManagementService tokenManagementService;


    public RegistrationService(UsersRepository usersRepository, TokenManagementService tokenManagementService) {
        this.usersRepository = usersRepository;
        this.tokenManagementService = tokenManagementService;
    }

    public String registerUser(UserDTO user){
        if (usersRepository.existsByEmail(user.getEmail())){
            throw new UserExistsException("UserDTO exists exception");
        }
        User userEntity = new User();
        userEntity.setFullName(user.getFullName());
        userEntity.setEmail(user.getEmail());
        userEntity.setPassword(new HashingService().hashPassword(user.getPassword()));
        userEntity.setRole(user.getRole());
        usersRepository.addUser(userEntity);
        return tokenManagementService.createToken(
                usersRepository.findUserIdByEmail(userEntity.getEmail()),
                userEntity.getFullName());
    }

}
