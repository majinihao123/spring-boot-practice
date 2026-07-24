package com.example.demo.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log
            = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminUserInitializer(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username}") String adminUsername,
            @Value("${app.bootstrap-admin.password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminPassword)) {
            log.warn(
                    "未设置 APP_ADMIN_PASSWORD，跳过初始管理员创建"
            );
            return;
        }

        if (userRepository.findByUsername(adminUsername).isPresent()) {
            log.info(
                    "管理员账号 {} 已存在，跳过创建",
                    adminUsername
            );
            return;
        }

        String passwordHash
                = passwordEncoder.encode(adminPassword);

        AppUser admin = new AppUser(
                adminUsername,
                passwordHash,
                "ADMIN",
                true
        );

        userRepository.save(admin);

        log.info(
                "初始管理员账号 {} 创建成功",
                adminUsername
        );
    }
}
