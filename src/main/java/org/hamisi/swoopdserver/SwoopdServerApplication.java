package org.hamisi.swoopdserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SwoopdServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwoopdServerApplication.class, args);
    }

}
