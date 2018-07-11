package com.msa.order.service;

import com.msa.order.api.CustomerClient;
import org.springframework.stereotype.Service;
@Service
public class CustomerService {
    private CustomerClient customerClient;
    public CustomerService(CustomerClient customerClient) {
        this.customerClient = customerClient;
    }
    public String getCustomer(){
        return customerClient.getCustomer();    // CustomerClient를 이용하여 서비스 호출
    }
}
