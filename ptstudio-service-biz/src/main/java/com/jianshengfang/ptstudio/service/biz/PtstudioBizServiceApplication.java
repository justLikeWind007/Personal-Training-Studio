package com.jianshengfang.ptstudio.service.biz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.jianshengfang.ptstudio")
@EnableScheduling
@ComponentScan(
        basePackages = "com.jianshengfang.ptstudio",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.jianshengfang\\.ptstudio\\.core\\.adapter\\.ops\\..*Controller"
        )
)
public class PtstudioBizServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PtstudioBizServiceApplication.class, args);
    }
}
