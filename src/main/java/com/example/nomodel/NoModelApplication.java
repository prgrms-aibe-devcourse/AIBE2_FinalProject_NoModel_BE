package com.example.nomodel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NoModelApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoModelApplication.class, args);
    }

}
