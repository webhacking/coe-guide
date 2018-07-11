package com.msa.order;

import com.msa.order.service.CustomerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableDiscoveryClient
@RestController
@SpringBootApplication
public class OrderApplication {

    private CustomerService customerService;

    public OrderApplication(CustomerService customerService) {
        this.customerService = customerService;
    }

    @RequestMapping(value = "orders")
    public String getOrder() {
        return customerService.getCustomer() + "'s order list";
    }

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
