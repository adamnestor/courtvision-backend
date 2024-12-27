package com.adamnestor.courtvision.test.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
        }
)
@ActiveProfiles("test")
@Import(TestConfig.class)
public class DatabaseConnectionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testDatabaseConnection() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result);
    }
}
