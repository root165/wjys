package com.wjys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.wjys.module")
public class WjysApplication {

    public static void main(String[] args) {
        SpringApplication.run(WjysApplication.class, args);
    }
}
