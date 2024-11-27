package com.adamnestor.courtvision.test.repository;

import com.adamnestor.courtvision.test.config.UserBaseTestSetup;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class UsersRepositoryTest extends UserBaseTestSetup {

    @Test
    void testFindByEmail() {
        assertThat(usersRepository.findByEmail("test@example.com"))
                .isPresent()
                .hasValueSatisfying(user ->
                        assertThat(user.getEmail()).isEqualTo("test@example.com")
                );
    }

    @Test
    void testFindByEmailNotFound() {
        assertThat(usersRepository.findByEmail("nonexistent@example.com"))
                .isEmpty();
    }

    @Test
    void testExistsByEmail() {
        assertThat(usersRepository.existsByEmail("test@example.com"))
                .isTrue();
    }

    @Test
    void testExistsByEmailNotFound() {
        assertThat(usersRepository.existsByEmail("nonexistent@example.com"))
                .isFalse();
    }
}