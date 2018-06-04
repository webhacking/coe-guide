# Order Service

## Description

본 프로젝트는 Microservice를 위한 Sample Project 입니다.

## Require Software

### RabbitMQ on docker
~~~
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 --restart=unless-stopped -e RABBITMQ_DEFAULT_USER=username -e RABBITMQ_DEFAULT_PASS=password rabbitmq:management
~~~

## Feign Client
 - REST 기반 서비스 호출을 추상화한 Spring Cloud Netflix 라이브러리
 - 인터페이스를 통해 클라이언트 측 프로그램 작성

### 설치
1. pom.xml에 feign dependency 추가
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-feign</artifactId>
    <version>Version!</version>
</dependency>
```

2. @EnableFeignClients annotation 추가

  ```java
  @SpringBootApplication
  @EnableFeignClients
  public class Application {

      public static void main(String[] args) {
          SpringApplication.run(Application.class, args);
      }

  }
  ```

3. Client Interface 생성
```java
@FeignClient(
        name ="CUSTOMER-SERVICE",
        url = "http://testhost:portnumber",
        decode404 = true
)
public interface CustomerClient {
    @RequestMapping(method = RequestMethod.GET, value = "/customers")
    List<Customer> findAll();
}
```

  - name : 서비스ID 혹은 논리적인 이름, spring-cloud의 eureka, ribbon에 사용
  - url : 실제 호출할 서비스의 URL, eureka, ribbon을 사용하지 않고서도 동작
  - decode404 : 404응답이 올 때 FeignExeption을 발생시킬지, 아니면 응답을 decode할 지 여부
