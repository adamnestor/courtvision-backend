package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.domain.UserRole;
import com.adamnestor.courtvision.domain.UserStatus;
import com.adamnestor.courtvision.repository.UserPicksRepository;
import com.adamnestor.courtvision.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
public class UserBaseTestSetup extends BaseTestSetup {

    @Autowired
    protected UserPicksRepository userPicksRepository;

    @Autowired
    protected UsersRepository usersRepository;

    protected Users testUser;

    @BeforeEach
    void userSetup() {
        // Clean up user-related data
        userPicksRepository.deleteAll();
        usersRepository.deleteAll();

        // Important: Call super.setup() first to ensure base test data is created
        super.setup();

        // Create test user
        testUser = new Users();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword123");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setLastLogin(LocalDateTime.now());
        testUser = usersRepository.save(testUser);
    }
}