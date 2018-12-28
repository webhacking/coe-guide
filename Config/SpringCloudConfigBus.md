# Spring Cloud Bus

Spring Cloud Bus 는 분산 시스템에 존재하는 노드들을 경량 메시지 브로커(rabbitmq, kafka etc)와 연결하는 역할을 합니다.

구성 변경과 같은 상태변경, 기타관리 등을 브로드캐스트하는데 사용이 가능합니다.

현재 AMQP 브로커를 전송으로 사용하지만 Kafka, Redis도 사용 할 수 있습니다. 그 외의 전송은 아직 지원되지 않습니다.


## 1. 개요
* Spring Cloud Config Server를 구축하게 되면 각 어플리케이션에 대한 설정정보(ex: applicatoin.yml)를 한 곳에서 관리 할 수 있습니다.

* 하지만 해당 정보가 수정 될 경우 클라이언트 어플리케이션을 재기동해야 하는 것은 변함이 없습니다.

* 이러한 방식은 이상적이지 않기 때문에 spring-boot-actuator와 @RefreshScope 어노테이션을 추가한 이후에 해당 클라이언트에 아래와 같은 명령을 보내어 재기동 없이 설정정보를 다시 읽어오게 할 수 있습니다.
~~~bash
$ curl -x POST http://[ip]:[port]/refresh
~~~

* 하지만 클라우드 환경에서는 모든 actuator endpoint에 접근하여 모든 클라이언트 어플리케이션을 refresh 해야 하는 번거로움이 존재합니다. 이러한 문제는 Spring Cloud Bus를 통해서 해결 할 수 있습니다.

* 아래와 같은 서버(또는 브로커)를 만들도록 하겠습니다.
  * hello-act-client
  * config-server
  * RabbitMQ(Docker)

## 2. hello-act-client 구축
  해당 어플리케이션은 GET request 를 통해 간단한 문자열을 출력하는 어플리케이션입니다.

먼저 간단한 dependency를 추가합니다.
(해당 프로젝트는 spring-boot-starter-parent:2.0.2.RELEASE를 사용합니다.)
~~~xml
<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-web</artifactId>
</dependency>
~~~

application.yml을 작성합니다.
여기서 message.act 는 GET request 호출 시 출력할 문자열입니다.
~~~yml
server:
  port: 8090

message:
  act: "act"

spring:
  application:
    name: hello-act
~~~

Controller를 작성합니다.
~~~java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloActController {
    @Value("${message.act}")
    private String message;
    @GetMapping("/")
    public String getMessage() {
        return message;
    }
}
~~~

서버를 기동하고 terminal에서 GET request를 호출하면 "act" 라는 메시지가 출력 되는 것을 확인 할 수 있습니다.
~~~bash
$ curl -X GET http://localhost:8090/
act
~~~

## 3. config-server 구축
해당 message를 config 서버에서 읽어와서 출력하기 위해서는 config-server 구축이 필요합니다.
새로운 프로젝트를 생성하고 아래와 같은 dependency를 추가합니다.
~~~xml
<parent>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-parent</artifactId>
	<version>2.0.2.RELEASE</version>
	<relativePath/>
</parent>

<properties>
	<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
	<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
	<java.version>1.8</java.version>
	<spring-cloud.version>Finchley.RELEASE</spring-cloud.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>${spring-cloud.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  ...
	<dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-config-server</artifactId>
	</dependency>
  ...
</dependencies>
~~~

해당 어플리케이션이 config-server임을 알리기 위해서 @EnablieConfigServer 어노테이션을 추가합니다.
~~~java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}
}
~~~

해당 서버가 바라보는 config 저장소를 application.yml에 추가합니다.
해당 예제에서는 git 주소를 로컬 git으로 하였습니다.
~~~ yml
server:
  port: 8091

spring:
  cloud:
    config:
      server:
        git:
          uri: file:/Users/bristol/bradley/configure
~~~

이제 /Users/bristol/bradley/configure 경로에 hello-act-client에서 사용 할 설정 정보를 가져옵니다.

hello-act.yml을 만들어 아래와 같은 설정 정보를 넣습니다.
~~~yml
server:
  port: 8090

message:
  act: "act"
~~~

이 후에 commit 을 해주도록 합니다.

~~~bash
$ git add.
$ git commit -m 'init yml'
~~~

## 4. hello-act-client 수정
config-server를 구축하였으므로 이제 포트정보와 message 정보는 config-server를 통해서 가져오도록 하겠습니다.

먼저 cloud-config를 사용 할 수 있도록 dependency를 추가합니다.

~~~xml
<properties>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
  <java.version>1.8</java.version>
  <spring-cloud.version>Finchley.RELEASE</spring-cloud.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>${spring-cloud.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
	<dependency>
    ...
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-config-client</artifactId>
	</dependency>
  ...
</dependencies>
~~~

기존의 application.yml 내용 중 아래의 내용을 bootstrap.yml으로 이동하고 application.yml을 삭제합니다.

~~~yml
spring:
  application:
    name: hello-act
  cloud:
    config:
      uri: http://localhost:8091
~~~

해당 서버를 재기동 한 이후 아래와 같이 명령어를 보내면 act 라는 문자열이 출력되는 것을 볼 수 있습니다.
~~~bash
$ curl -X GET http://localhost:8090/
act
~~~

configure 폴더에서 메시지 정보를 수정합니다.
~~~yml
message:
  act: "hello-act"
~~~

다시 commit을 합니다.
~~~bash
$ git add .
$ git commit -m 'change message'
~~~

서버 재 기동 없이 아래와 같이 명령어를 보내면 여전히 act 라는 문자열이 출력되는 것을 볼 수 있습니다.
~~~bash
$ curl -X GET http://localhost:8090/
act
~~~

## 5. RefreshScope
환경설정을 바꿨다고 해서 서버를 재기동하는 것은 불필요한 행위입니다. 따라서 서버 재기동 없이 환경설정을 읽어오는 방법을 알아보겠습니다.

먼저 hello-act-client 어플리케이션에 dependency를 추가합니다.

~~~xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
~~~

@RefreshScope 어노테이션을 추가합니다.
@RefreshScope로 표시된 Spring Bean은 사용시 초기화 되는 lazy proxy로 범위는 초기화 된 캐쉬 값으로 작동합니다.
~~~java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class HelloActController {
    @Value("${message.act}")
    private String message;
    @GetMapping("/")
    public String getMessage() {
        return message;
    }
}
~~~

그리고 bootstrap.yml에 아래와 같은 설정을 추가합니다.
(기본적으로 actuator로 추가 된 민감한 엔드포인트는 보안에 묶여 있습니다. 아래와 같은 설정을 하거나 특정 url을 노출 시킬 수 있습니다.)

~~~yml
management:
  security:
    enabled: false
~~~

서버를 재기동하고 아래와 같이 명령어를 보내면 hello-act가 출력됩니다.
~~~bash
$ curl -X GET http://localhost:8090/
hello-act
~~~

config저장소에서 hello-act.yml을 열어 hello-sds로 변경하고 commit을 합니다.
~~~yml
message:
  act: "hello-sds"
~~~

~~~bash
$ git add.
$ git commit -m 'change sds'
~~~

이제 서버 재기동 없이 터미널에서 아래와 같은 명령어를 보냅니다.
~~~bash
$ curl -X POST http://localhost:8090/actuator/refresh
~~~

해당 명령어를 보내면 변경된 프로퍼티가 출력 됩니다.
다시 아래와 같은 명령어를 날리게 되면 변경된 메시지가 출력 되는 것을 확인 할 수 있습니다.

~~~bash
$ curl -X GET http://localhost:8090/
hello-sds
~~~


## 6. Spring Cloud Bus
변경된 설정 값이 반영 되는 것을 확인하였으나 이와 같은 방법은 클라우드환경에서 endpoint 가 늘어날 수록 번거로울수 밖에 없습니다.
따라서 Spring Cloud Bus를 사용해보도록 합니다.

먼저 rabbitmq를 docker로 실행합니다.
~~~bash
$ docker run -d \
  --hostname rabbit \
  --name rabbit \
  -p 15672:15672 \
  -p 5672:5672 \
  rabbitmq:3.7.5-management
~~~

클라이언트 어플리케이션에 아래와 같은 dependency를 추가합니다.
~~~xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
~~~

config 저장소에 hello-act.yml을 아래의 구문을 추가합니다.

~~~yml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
~~~


이제 config-server 설정을 변경합니다.
~~~xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-monitor</artifactId>
</dependency>

<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
~~~

다음으로 config-server의 application.yml을 수정합니다.

~~~xml
spring:
  ...
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
management:
  endpoints:
    web:
      exposure:
        include: "bus-refresh"
~~~

이제 모든 서버를 재 기동 한 이후에 hello-act.yml의 메시지 값을 다시 변경합니다.
~~~yml
...
message:
  act: "hello-sds-act"
...
~~~

기존에는 hello-act 서버에 리퀘스트를 보냈으나
이제는 config-server에 리퀘스트를 보냅니다.
~~~bash
$ curl -X POST http://localhost:8091/actuator/bus-refresh
~~~

이후에 hello-act 서버에 GET 리퀘스트를 보내면 변경된 메시지를 확인 할 수 있습니다.
~~~bash
$ curl http://localhost:8090/
hello-sds-act
~~~

이러한 방법을 사용하면 코드 저장소(github, gitlab, bitbucket 등)에 webhook 기능을 사용하여 설정파일 변경이후에 commit, push가 일어날 때마다 자동으로 모든 클라우드 노드의 refrshscope 가 적용된 어플리케이션이 환경설정을 다시 읽게 할 수 있습니다.
