package com.jianshengfang.ptstudio.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.jianshengfang.ptstudio")
@MapperScan("com.jianshengfang.ptstudio.core.infrastructure")
@EnableScheduling
public class PtstudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PtstudioApplication.class, args);
    }
}
