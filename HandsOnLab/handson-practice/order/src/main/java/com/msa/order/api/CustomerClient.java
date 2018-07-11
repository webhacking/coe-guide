package com.msa.order.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(
        name ="CUSTOMER-SERVICE",   // eureka에 등록된 instance명으로 서비스 조회
        decode404 = true    // 404 에러 발생시 feign 자체 에러 발생 안함
)
@Component
public interface CustomerClient {

    @RequestMapping(method = RequestMethod.GET, value = "/customer")    // customer-service의 customer api 호출
    String getCustomer();
}
