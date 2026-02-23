package com.jianshengfang.ptstudio.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class PtstudioRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(PtstudioRegistryApplication.class, args);
    }
}
