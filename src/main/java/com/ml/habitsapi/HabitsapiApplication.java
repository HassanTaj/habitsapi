package com.ml.habitsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HabitsapiApplication {
    // Spring starts the web server here and finds the rest of the app below this package.
    public static void main(String[] args) {
        SpringApplication.run(HabitsapiApplication.class, args);
    }
}
