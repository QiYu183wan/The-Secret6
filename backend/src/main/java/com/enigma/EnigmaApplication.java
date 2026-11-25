package com.enigma;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.enigma.mapper")
public class EnigmaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnigmaApplication.class, args);
    }
}
