package com.jianshengfang.ptstudio.service.ops;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.jianshengfang.ptstudio")
@EnableScheduling
@EnableDiscoveryClient
@ComponentScan(
        basePackages = "com.jianshengfang.ptstudio",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.jianshengfang\\.ptstudio\\.core\\.adapter\\.(?!ops\\.).*Controller"
        )
)
public class PtstudioOpsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PtstudioOpsServiceApplication.class, args);
    }
}
