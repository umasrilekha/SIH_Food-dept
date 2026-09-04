package com.govmesh.food;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FoodDepartmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodDepartmentApplication.class, args);
    }
}
