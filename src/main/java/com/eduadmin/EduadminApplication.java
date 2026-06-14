package com.eduadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.eduadmin.repository")
public class EduadminApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduadminApplication.class, args);
    }
}
