package com.think;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author 好市民
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class ThinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThinkApplication.class, args);
    }

}
