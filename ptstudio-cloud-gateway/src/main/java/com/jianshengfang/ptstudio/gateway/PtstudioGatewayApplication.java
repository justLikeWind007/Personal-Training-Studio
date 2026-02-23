package com.jianshengfang.ptstudio.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PtstudioGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PtstudioGatewayApplication.class, args);
    }
}
