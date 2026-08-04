package org.hamisi.swoopdserver.config;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.auth.services.HashingService;
import org.hamisi.swoopdserver.users.Role;
import org.hamisi.swoopdserver.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SuperAdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UsersRepository usersRepository;
    private final HashingService hashingService;
    private final String superAdminEmail;
    private final String superAdminPassword;
    private final String superAdminName;

    public SuperAdminInitializer(
            UsersRepository usersRepository,
            HashingService hashingService,
            @Value("${SUPER_ADMIN_EMAIL}") String superAdminEmail,
            @Value("${SUPER_ADMIN_PASSWORD}") String superAdminPassword,
            @Value("${SUPER_ADMIN_NAME}") String superAdminName) {
        this.usersRepository = usersRepository;
        this.hashingService = hashingService;
        this.superAdminEmail = superAdminEmail;
        this.superAdminPassword = superAdminPassword;
        this.superAdminName = superAdminName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usersRepository.existsByEmail(superAdminEmail)) {
            log.debug("Super admin account already exists — skipping seed.");
            return;
        }

        User superAdmin = new User();
        superAdmin.setFullName(superAdminName);
        superAdmin.setEmail(superAdminEmail);
        superAdmin.setRole(Role.ADMIN);
        superAdmin.setPassword(hashingService.hashPassword(superAdminPassword));

        usersRepository.save(superAdmin);
        log.info("Super admin account created: {}", superAdminEmail);
    }
}

