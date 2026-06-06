package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.dtos.UserDTO;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.auth.exceptions.UserExistsException;
import org.hamisi.swoopdserver.users.User;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final UsersRepository usersRepository;

    public RegistrationService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public void registerUser(UserDTO user){
        if (usersRepository.existsByEmail(user.getEmail())){
            throw new UserExistsException("UserDTO exists exception");
        }
        User userEntity = new User();
        userEntity.setFullName(user.getFullName());
        userEntity.setEmail(user.getEmail());
        userEntity.setPassword(new HashingService().hashPassword(user.getPassword()));
        userEntity.setRole(user.getRole());
        usersRepository.addUser(userEntity);
    }

}
