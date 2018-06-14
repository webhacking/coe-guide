# TODO
- 개요 보강
- 2.0 version 설정 상세 보강
- Hystrix, Turbine integration 설정 상세 보강 (1.5x 버전 한정)

# 1. 개요

Spring Boot Admin 은 Spring Boot 어플리케이션의 admin interface 를 제공하는 UI 프레임워크이다.

### 제공하는 기능
- Health Status
- Application 상세 정보
    - JVM & memory metrics
    - Datasource metrics
    - Cache metrics 등
- 빌드 정보
- Spring Boot Actuator 제공 정보
- Hystrix stream 정보 등

# 2. 구성방법

### version : 1.5x

> 참고 : http://codecentric.github.io/spring-boot-admin/1.5.7/

##### Server-side 구성

1. pom.xml

```xml
	<dependencyManagement>
		<dependencies>
            <dependency>
				<groupId>de.codecentric</groupId>
				<artifactId>spring-boot-admin-dependencies</artifactId>
				<version>${spring-boot-admin.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
		<java.version>1.8</java.version>
		<spring-boot-admin.version>1.5.7</spring-boot-admin.version>
		<spring-cloud.version>Edgware.RC1</spring-cloud.version>
	</properties>

    <dependencies>
		<!-- for Spring Boot Admin -->
		<dependency>
			<groupId>de.codecentric</groupId>
			<artifactId>spring-boot-admin-starter-server</artifactId>
		</dependency>
		<dependency>
			<groupId>de.codecentric</groupId>
			<artifactId>spring-boot-admin-server-ui</artifactId>
		</dependency>
		<dependency>
			<groupId>de.codecentric</groupId>
			<artifactId>spring-boot-admin-server-ui-hystrix</artifactId>
		</dependency>
		<dependency>
			<groupId>org.jolokia</groupId>
			<artifactId>jolokia-core</artifactId>
		</dependency>

		<!-- Spring Boot -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<!-- Spring Cloud -->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
		</dependency>
	</dependencies>
```

2. application.yml
```yaml
spring:
  application:
    name: coe-admin-server

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: ALWAYS
  security:
    enabled: false

server:
  port: 8080

eureka:
  instance:
    leaseRenewalIntervalInSeconds: 10
  client:
    registryFetchIntervalSeconds: 5
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
    enabled: true
    ...

```

3. add @EnableAdminServer Annotation

```java
@SpringBootApplication
@EnableDiscoveryClient
@Configuration
@EnableAutoConfiguration
@EnableAdminServer
public class CoeAdminApplication {
s	public static void main(String[] args) {
		SpringApplication.run(CoeAdminApplication.class, args);
	}
}
```

4. customize security configuration
```java
	@Configuration
	public static class SecurityPermitAllConfig extends WebSecurityConfigurerAdapter {
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().anyRequest().permitAll()
					.and().csrf().disable();
		}
	}
```

##### Client-side 구성 (with Eureka Client)

각 마이크로서비스가 Eureka Client로 구성되어 있는 경우, Actuator 정보만 추가하여 Admin Server에 자동 등록하고 사용할 수 있다.

> Eureka 는 필수가 아니며, Spring Boot Admin Client 를 통해 구성 가능하다.

1. pom.xml
```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.jolokia</groupId>
        <artifactId>jolokia-core</artifactId>
    </dependency>
```

2. application.yml
```yaml
server:
  port: 17003
spring:
  application:
    name: order-service

eureka:
  instance:
    leaseRenewalIntervalInSeconds: 10
    # healthCheckUrl: http://localhost:17003/actuator/health
    # statusPageUrl: http://localhost:17003/actuator/info
    prefer-ip-address: true
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
    enabled: true

# Spring Admin Server 를 위한 설정 #
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: ALWAYS
#  server:
#    servlet:
#      context-path: /actuator
```

3. customize security configuraion
```java
@Configuration
@EnableWebSecurity
public class SecurityPermitAllConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().permitAll()
                .and().csrf().disable();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/**");
    }
}
```

#### version : 2.0x

> 참고 : https://codecentric.github.io/spring-boot-admin/current/

###### 서비스측 구성 (with Eureka)

# 참고
SpringBoot Admin Sever의 버전과 마이크로 서비스의 SpringBoot(Actuator) 버전에 따라 이슈가 있음
1. SpringBoot Admin Sever : 2.0.0
    Eureka 에 등록된 서버 정보를   SpringBoot Admin에 등록하지 못하는 문제 (springBoot-admin-client를 통해서는 등록 됨)
    (참고 : https://github.com/codecentric/spring-boot-admin/issues/776)

    ==> Spring Boot 2.0.1-SNAPSHOT 사용 권장

2. SpringBoot Admin Sever : 2.0.1-SNAPSHOT
    - admin server issue
        spring-boot-admin-server-ui-hystrix, spring-boot-admin-server-ui-turbine의 최종 버전 1.5.8 이고
        Spring Admin 2.x에서 해당 버전을 지원하지 않음(추후 계획 없음)
        (참고 : https://github.com/codecentric/spring-boot-admin/issues/657)
    - client issue
        마이크로 서비스가 Spring Boot 1.x 버전을 사용할 경우 Admin Server에서 모든 Endpoint 지원하지 않음
        (참고 : http://codecentric.github.io/spring-boot-admin/2.0.0/)
        >As some of the actuator endpoints changed with the Spring Boot 2 release not all options might be available (e.g. /metrics endpoint); for some of the endpoints we provide legacy converters.

3. SpringBoot Admin Server : 1.5.7
    - client issue
        마이크로 서비스가 Spring Boot 2.x 버전을 사용할 경우 Admin Server에서 모든 Endpoint 지원하지 않음

> SpringBoot Admin Server와 각 마이크로 서비스의 버전을 맞춰서 사용하는 것을 권장.
> SpringBoot Admin 2.x를 사용하는 경우 turbine은 별도의 서비스로 제공 해야 함