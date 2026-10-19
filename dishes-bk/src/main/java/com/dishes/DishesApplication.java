package com.dishes;

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
public class DishesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DishesApplication.class, args);
    }

}
