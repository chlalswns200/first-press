package com.firstpress;

import org.springframework.boot.SpringApplication;

public class TestFirstPressApplication {

    public static void main(String[] args) {
        SpringApplication.from(FirstPressApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
