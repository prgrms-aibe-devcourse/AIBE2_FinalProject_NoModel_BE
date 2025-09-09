package com.example.nomodel;

import com.example.nomodel._core.config.TestOAuth2Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestOAuth2Config.class)
class NoModelApplicationTests {

    @Test
    void contextLoads() {
    }

}
