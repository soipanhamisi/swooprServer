package org.hamisi.swoopdserver.auth.services;

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

    public void registerUser(User user){
        if (usersRepository.existsByEmail(user.getEmail())){
            throw new UserExistsException("User exists exception");
        }
        user.setPassword(new HashingService().hashPassword(user.getPassword()));
        usersRepository.addUser(user);
    }
}
