package com.nightout;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "nightout.preload.enabled=false")
@Import(TestcontainersConfiguration.class)
class NightoutApplicationTests {

    @Test
    void contextLoads() {
        // Also proves the Flyway schema matches the entities (ddl-auto: validate)
    }
}
