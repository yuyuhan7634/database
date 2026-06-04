package org.example.cloud.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 🌟 1. 引入定时任务包

@SpringBootApplication
@EnableScheduling
public class EstateApplication {
    public static void main(String[] args) {
        SpringApplication.run(EstateApplication.class, args);
    }
}