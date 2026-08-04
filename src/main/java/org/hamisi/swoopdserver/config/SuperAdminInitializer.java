package org.hamisi.swoopdserver.config;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.auth.services.HashingService;
import org.hamisi.swoopdserver.users.Role;
import org.hamisi.swoopdserver.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private static final String SUPER_ADMIN_EMAIL    = "soipanhani@gmail.com";
    private static final String SUPER_ADMIN_PASSWORD = "password123";
    private static final String SUPER_ADMIN_NAME     = "Super Admin";

    private final UsersRepository usersRepository;
    private final HashingService hashingService;

    public SuperAdminInitializer(UsersRepository usersRepository, HashingService hashingService) {
        this.usersRepository = usersRepository;
        this.hashingService = hashingService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usersRepository.existsByEmail(SUPER_ADMIN_EMAIL)) {
            log.debug("Super admin account already exists — skipping seed.");
            return;
        }

        User superAdmin = new User();
        superAdmin.setFullName(SUPER_ADMIN_NAME);
        superAdmin.setEmail(SUPER_ADMIN_EMAIL);
        superAdmin.setRole(Role.ADMIN);
        superAdmin.setPassword(hashingService.hashPassword(SUPER_ADMIN_PASSWORD));

        usersRepository.save(superAdmin);
        log.info("Super admin account created: {}", SUPER_ADMIN_EMAIL);
    }
}

