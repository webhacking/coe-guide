package com.msa.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableDiscoveryClient
@RestController
@SpringBootApplication
public class CustomerApplication {

    @RequestMapping(value = "/customer")
    public String getCustomer() {
        return "John";
    }
    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
    }
}
