# 1. 개요
## Eureka란?
서비스 인스턴스 목록과 그 위치(host, port)가 동적으로 변하는 환경에서 사용자가 그 위치를 모두 관리하기는 어렵다.  
Eureka를 사용하면 등록된 모든 서비스의 정보를 registry로 관리하고, 이에 대한 접근 정보를 요청하는 서비스에게 목록을 제공한다.
- Eureka Server: Eureka Service가 자기 자신을 등록(Service Registration)하는 서버이자 Eureka Client가 가용한 서비스 목록(Service Registry)을 요청하는 서버.
- Eureka Client: 서비스들의 위치 정보를 Eureka Server로부터 fetch하는 서비스   

## Eureka기능
<p align="center"><img height="" src="../images/eureka-high-level-architecture.png"></p>

#### Register
- eureka.instance, eureka.client 설정값을 바탕으로 Eureka에 등록하기 위한 Eureka Instance 정보를 만듦
- Client가 eureka 서버로 첫 hearbeat 전송 시 Eureka Instance 정보를 등록
- 등록된 instance 정보는 eureka dashboard나  http://eurekaserver/eureka/apps를 통해 확인할 수 있음

#### Renew
- Client는 eureka에 등록 이후 설정된 주기마다 heatbeat를 전송하여 자신의 존재를 알림
  > eureka.instance.lease-renewal-interval-in-seconds (default: 30)  

- 설정된 시간동안 heartbeat를 받지 못하면 해당 Eureka Instance를 Registry에서 제거
  > eureka.instance.lease-expiration-duration-in-seconds  (default: 90)

- renew 관련 interval은 변경하지 않는것을 권장 함(서버 내부적으로 client를 관리하는 로직 때문)

#### Fetch Registry
- Client는 Server로부터 Registry(서버에 등록된 인스턴스 목록) 정보를 가져와서 로컬에 캐시
- 캐시 된 정보는 설정된 주기마다 업데이트 됨
  > eureka.client.registryFetchIntervalSeconds (default: 30)  

#### Cancel
- Client가 shutdown될 때 cancel 요청을 eureka 서버로 보내서 registry에서 제거 하게 됨

#### Time Lag
- Eureka server와 client의 registry 관련 캐시 사용으로 인해 client가 호출 하려는 다른 instance 정보가 최신으로 갱신되는데 약간의 시간 차가 있음

## Peering
여러대의 eureka server를 사용하여 서로 peering 구성이 가능하다.  
Eureka server는 설정에 정의된 peer nodes를 찾아서 Registry 정보 등 Sync 맞추는 작업을 한다 . 
- 관련 설정
  - Standalone으로 구성하려면 아래 처럼 설정  
    > eureka.client.register-with-eureka: false  
    
  - Peer nodes 로부터 registry를 갱신할 수 없을 때 재시도 횟수 //TODO: 상세 의미 파악 필요
    > eureka.server.registry-sync-retrires (default: 5)  

  - Peer nodes 로부터 registry를 갱신할 수 없을때 재시도를 기다리는 시간   //TODO: 상세 의미 파악 필요
    > eureka.server.wait-time-in-ms-when-sync-empty (default: 3000) milliseconds


## Self-Preservation Mode(자가보존모드)
Eureka 서버는 등록된 instance로부터 heartbeat를 주기적으로 받는다.  
하지만 네트워크 단절 등의 상황으로 hearbeat를 받을 수 없는 경우 보통 registry에서 해당 instance를 제거 한다.  

Eureka로의 네트워크는 단절되었지만, 해당 서비스 API를 호출하는데 문제가 없는 경우가 있을수 있어서,   
self-preservation 을 사용하여 registry에서 문제된 instance를 정해진 기간 동안 제거하지 않을 수 있다.  

EvictionTask가 매분 마다 Expected heartbeats 수와 Actual heartbeats 수를 비교하여 Self-Preservation 모드 여부를 결정한다.   
> eureka.server.eviction-interval-timer-in-ms (default: 60 * 1000)

#### Expected heartbeats updating scheduler
기본 매 15분(renewal-threshold-update-interval-ms) 마다 수행되며 preservation mode로 가기 위한 임계값을 계산한다.    
예를 들어 인스턴스 개수가 N개이고, renewal-percent-threshold값이 0.85이면 계산식은 아래와 같다.      
- 최소 1분이내 받아야 할 heartbeat 총 수 = 2 * N * 0.85  
  > 위 값은 아래 설정으로 변경 가능
  > eureka.instance.lease-renewal-interval-in-seconds (default: 30)  
  > eureka.server.renewal-percent-threshold (default: 0.85)  
  > scheduler 수행 주기 설정
  > eureka.server.renewal-threshold-update-interval-ms (default: 15 * 60 * 1000)

#### Actual heartbeats calculation scheduler
기본 매 1분 마다 수행되며 실제 받은 heartbeats 횟수를 계산하다.


// TODO: 클로이와 내용 확인 필요
#### Eureka Client 동작과 Server간 Communication
- Self-Identification & Registration
  - Instance Startup 이후 Instance 정보 Replication
    - 등록 이후 Instance 정보가 변경 되었을 때 Registry 정보를 갱신하기 위한 REST를 eureka.client.instance-info-replication-interval-seconds에 설정된 주기마다 호출한다 (default: 30)
    - eureka.client.initial-instance-info-replication-interval-seconds (default: 40)
  - Instance Startup 이후 Eureka server info refresh
    - Eureka Server 추가, 변경, 삭제가 일어날 때 Eureka Client가 얼마나 자주 service urls를 갱신할 것인지 eureka.client.eureka-service-url-poll-interval-seconds 값으로 조정할 수 있다 (default: 0, 단 DNS를 통해 service urls를 가져오는 경우)

- eureka.instance.instance-enabled-onit 설정값을 통해 Startup 후 Traffic 받을 준비가 되었을 때 status:UP이 되도록 할 수 있다 (default: false)

## Eureka registry caching [참조](https://blog.asarkar.org/technical/netflix-eureka/)  
- Eureka Server Response Cache 설정  
Eureka server에서 eureka client에게 자신의 registry 정보를 제공 시 사용하는 cache.  
client에게 더 빠른 registry 정보 제공을 위해 실제 registry 값이 아닌 cache의 값을 제공 함.  
```yml
eureka.server.response-cache-update-interval-ms: 3000 # 기본 30초
```
- Eureka Client Cache 설정  
Eureka client에 존재하는 cache로 eureka server에 서비스 정보 요청 시 이 cache의 값을 이용 함   
eureka.client.fetchRegistry 값이 false이면 client cache는 적용되지 않음   
```yml
eureka.client.registryFetchIntervalSeconds: 3 # 기본 30초
```
> Ribbon cache 설정(cache를 정리하며 ribbon cache 도 포함하여 정리 함)  
> Zuul, Feign 에서 다른 서비스 호출 시 사용 되는 cache
> ```yml
> ribbon.ServerListRefreshInterval: 3000
> ```

# 2. 구성방법

## Eureka Server 설치방법
1. pom.xml에 Eureka Server dependency 추가

    ```xml
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
    ```

3. configuration - application.yml 수정

    ```yaml
    server:
      port: 8761
    spring:
      application:
        name: discovery-service
    eureka:
      instance:
        hostname: localhost
      client:
        registerWithEureka: false
        fetchRegistry: false
        serviceUrl:
          defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
      server:
        enable-self-preservation: true
    ```
    - localhost:8761 에 Eureka Server 설정
    - DefaultZone Url 설정을 통해 동일한 zone의 eureka server clustering 설정
    - 설정값 설명
      - eureka.client.registerWithEureka: 본인 서비스를 eureka 서버에 등록 할지 여부.(eureka는 서버이면서 client가 될 수도 있음)
      - eureka.client.fetchRegistry: client 서비스가 eureka 서버로 부터 서비스 리스트 정보를 local에 caching 할지 여부

4. @EnableEurekaServer Annotation 추가하여 Eureka Server Application으로 선언

    ```java
    @EnableEurekaServer
    @SpringBootApplication
    public class CoeEurekaApplication {

      public static void main(String[] args) {
        SpringApplication.run(CoeEurekaApplication.class, args);
      }
    }
    ```

5. Eureka server 구동 후 Eureka Dashboard 확인
  ![](../images/eureka-dashboard.png)
    - 웹브라우저에서 Eureka Server로 설정한 URL에 접속시 Eureka 콘솔 화면을 볼 수 있음  

## Eureka Client 설치방법

1. pom.xml에 Eureka Client dependency 추가
    ```xml
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        <version>1.4.4.RELEASE</version>
    </dependency>
    ```
2. property 추가
    ```yaml
    spring:
      application:
        name: customer-service

    eureka:
      client:
        serviceUrl:
          defaultZone: http://192.168.1.19:8761/eureka/
        enabled: true
      instance:
        preferIpAddress: true # 서비스간 통신 시 hostname 보다 ip 를 우선 사용 함
    ```
    - Eureka Server 연결 설정
    - Service 명 customer-service로 설정 (eureka server에 등록되는 서비스 명)

3. @EnableDiscoveryClient Annotation 추가하여 Eureka Client 선언

    ```java
    @EnableDiscoveryClient  //eureka, consul, zookeeper의 implements를 모두 포함. @EnableEurekaClient는 only works for eureka.
    @SpringBootApplication
    public class CustomerApplication {

        public static void main(String[] args) {
            SpringApplication.run(CustomerApplication.class, args);
        }

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }

    }
    ```

## Eureka Peering
- application.yml (Server)

  ```yaml
  server:
    port: 8761

  eureak:
    server:
      enable-self-preservation: true
    client:
      registerWithEureka: true      
      fetchRegistry: true           

  ---

  spring:
    profiles: eureka1
  eureka:
    instance:
      hostname: eureka1
      serviceUrl:
        defaultZone: http://eureka2:8761/eureka/

  ---
  spring:
    profiles: eureka2
  eureka:
    instance:
      hostname: eureka2
    client:
      serviceUrl:
        defaultZone: http://eureka1:8761/eureka/
  ```

  - 동일서버에서 실행하는 경우 instance hostname은 unique하게 설정되어야 한다.
  - registerWithEureka true로 설정
    - true설정시 서버 자신도 유레카 클라이언트로 등록한다.
  - fetchRegistry true로 설정
    - defaultZone의 유레카 서버에서 클라이언트 정보를 가져온다(registerWithEureka가 true로 설정되어야 동작함)
  - profile 추가하여 서로 참조하도록 serviceUrl.defaultZone 설정
  - self preservation [참조](https://medium.com/@fahimfarookme/the-mystery-of-eureka-self-preservation-c7aa0ed1b799)

- application.yml (Client)

  ```yaml
  spring:
    application:
      name: customer-service

  eureka:
    client:
      serviceUrl:
        defaultZone: http://eureka1:8761/eureka/,http://eureka2:8761/eureka/
      enabled: true
  ```
  - eureka.client.serviceUrl.defaultZone에 clustering한 유레카 서버 모두 입력
    - heart-beat는 defaultZone의 very first 항목인 eureka1에 만 전송
  - 여러개의 Eureka에 등록할 경우 defaultZone에 ,(comma)로 구분하여 입력한다.
