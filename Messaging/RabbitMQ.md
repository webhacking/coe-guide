# RabbitMQ 클러스터링

클러스터링은 여러 시스템을 연결하여 단일 논리 브로커를 형성하는 것을 의미한다. 서로 다른 노드에 존재하는 rabbitmq는 Erlang(이하 얼랭)을 통해서 통신을 하기 때문에 각 노드들은 동일한 얼랭 쿠키를 가져야 한다. (따라서 모든 노드들은 동일한 버전의 rabbitmq, 얼랭을 실행 해야 한다.)
virtual hosts, exchanges, users, permissions은 모든 노드에 자동으로 미러링 되지만 큐는 미러링 되지 않는다. 따라서 고 가용성을 위해 추가적인 미러링 작업이 필요하다
일부 분산 시스템에서는 리더 및 팔로워 노드가 존재하지만 RabbitMQ에서는 일반적으로 모든 노드가 동등한 피어로 간주 된다. 물론 큐 미러링과 플러그인을 고려할 때 더 미묘한 차이가 존재하지만 대부분의 의도와 목적을 위해 모든 클러스터 노드들은 동일하게 간주 된다.
해당 wiki는 일반적인 설치 방법과 도커를 이용한 방법을 설명한다.

## 1. 포트
  * 4369 - epmd, RabbitMQ 노드 및 CLI 도구에서 사용
  * 5672, 5671 - AMQP 0-9-1과 1.0 클라이언트가 사용하는 포트(TLS 적용에 따라 포트 변경
  * 25672 - 노드 및 CLI도구 통신에 사용되며 동적 범위에서 할당(기본적으로 단일 포트로 제한되며 AMPQ포트 + 20000으로 기본 할당)
  * 35672~35682 - 노드와의 통신을 위해 CLI 도구에서 사용되는 포트
  * 61613, 61614 : TLS가 있거나 없는 STOMP 클라이언트 (STOMP 플러그인이 활성화 된 경우에만 해당)
  * 1883, 8883 : (MQTT 플러그인이 사용 가능한 경우 TLS가없는 MQTT 클라이언트
  * 15674 : STOMP-over-WebSockets 클라이언트 (웹 STOMP 플러그인이 사용 가능한 경우에만)
  * 15675 : WebTockets-over-WebSockets 클라이언트 (Web MQTT 플러그인이 사용 가능한 경우에만)

## 2. 클러스터 구성하기 (일반 설치)
본 예제는 총 3개의 노드를 이용하여 클러스터를 구성한다.
### 2.1. 설치
~~~bash
$ wget http://www.rabbitmq.com/releases/rabbitmq-server/v3.6.14/rabbitmq-server-mac-standalone-3.6.14.tar.xz
$ tar -xzvf rabbitmq-server-mac-standalone-3.6.14.tar.xz
$ mv rabbitmq_server-3.6.14 rabbit1
$ cp -r rabbit1 rabbit2
$ cp -r rabbit1 rabbit3
~~~

### 2.2. 실행 및 노드 설정
~~~bash
$ RABBITMQ_NODE_PORT=5672 RABBITMQ_SERVER_START_ARGS="-rabbitmq_management listener [{port,15672}]" RABBITMQ_NODENAME=rabbit1 ./rabbitmq1/sbin/rabbitmq-server -detached
$ ./rabbitmq1/sbin/rabbitmq-plugins -n rabbit1 enable rabbitmq_management
$ ./rabbitmq1/sbin/rabbitmqctl -n rabbit1 start_app

$ RABBITMQ_NODE_PORT=5673 RABBITMQ_SERVER_START_ARGS="-rabbitmq_management listener [{port,15673}]" RABBITMQ_NODENAME=rabbit2 ./rabbitmq2/sbin/rabbitmq-server -detached
$ ./rabbitmq2/sbin/rabbitmq-plugins -n rabbit2 enable rabbitmq_management
$ ./rabbitmq2/sbin/rabbitmqctl -n rabbit2 start_app

$ RABBITMQ_NODE_PORT=5674 RABBITMQ_SERVER_START_ARGS="-rabbitmq_management listener [{port,15674}]" RABBITMQ_NODENAME=rabbit3 ./rabbitmq3/sbin/rabbitmq-server -detached
$ ./rabbitmq3/sbin/rabbitmq-plugins -n rabbit3 enable rabbitmq_management
$ ./rabbitmq3/sbin/rabbitmqctl -n rabbit3 start_app
~~~

localhost:15672/15673/15674에 접속하여 guest/guest로 확인

### 2.3 클러스터 구성
커맨드 라인 명령어 rabbitmqctl를 사용해 3개의 노드를 하나의 클러스터로 구성한다.
시작전 반드시 구성할 노드 실행을 중지한다.(중지는 실행의 역순)

~~~bash
$ ./rabbitmq3/sbin/rabbitmqctl -n rabbit3 stop_app
$ ./rabbitmq2/sbin/rabbitmqctl -n rabbit2 stop_app

$ ./rabbitmq2/sbin/rabbitmqctl -n rabbit2 join_cluster rabbit1@호스트네임
$ ./rabbitmq3/sbin/rabbitmqctl -n rabbit3 join_cluster rabbit1@호스트네임

$ ./rabbitmq2/sbin/rabbitmqctl -n rabbit2 start_app
$ ./rabbitmq3/sbin/rabbitmqctl -n rabbit3 start_app
~~~

localhost:15672/15673/15674에 접속하여 guest/guest로 확인

### 2.4 미러링 구성하기
위에까지 작업은 클러스터를 구성하긴 하였지만 고 가용성이(Highly Available)라고 하기에는 부족하다. 이유는 현재 3개의 클러스터는 서로의 데이터들을 공유할 뿐이지 큐 데이터 자체는 1번만 가지고 있기 때문이다.
만약 1번 노드가 죽게 된다면 2,3번 노드는 해당 메시지 큐를 읽을 수 없기 때문이다.

미러링 정책 설정
~~~bash
rabbitmqctl -n rabbit1 set_policy act-policy "^cluster" '{"ha-mode":"all"}'
~~~

* set policy act-policy > 정책 이름을 act-policy로 한다
* "^cluster" > 미러링할 queue 표현식
* '{"ha-mode":"all"}' > 클러스터 안의 모든 노드에 대해서 미러링을 한다는 뜻 자세한 것은 https://www.rabbitmq.com/ha.html 참조

## 3. 클러스터 구성하기 (도커 설치)
### 3.1. 설치 및 실행
주의 : 단순 예제이기 때문에 해당 컨테이너를 종료시킬 경우 데이터가 사라지게 된다. 따라서 -v 옵션을 이용하여 마운트를 하는 것이 좋다.

1 번 노드 실행
~~~bash
docker run -d \
  --hostname rabbit1 \
  --name rabbit1 \
  -e RABBITMQ_ERLANG_COOKIE='test' \
  -p 15682:15672 \
  -p 5682:5672 \
  rabbitmq:3.5.3-management
~~~

2번 노드 실행
~~~bash
docker run -d \
  --hostname rabbit2 \
  --name rabbit2 \
  --link rabbit1 \
  -e RABBITMQ_ERLANG_COOKIE='test' \
  -p 15683:15672 \
  -p 5683:5672 \
  rabbitmq:3.5.3-management
~~~

3번 노드 실행
~~~bash
docker run -d \
  --hostname rabbit3 \
  --name rabbit3 \
  --link rabbit1 \
  --link rabbit2 \
  -e RABBITMQ_ERLANG_COOKIE='test' \
  -p 15684:15672 \
  -p 5684:5672 \
  rabbitmq:3.5.3-management
~~~

### 3.2 클러스터링
~~~bash
$ docker exec -it rabbit3 bash
root@rabbit3 : /usr/sbin/rabbitmqctl stop_app
root@rabbit3 : rabbitmqctl join_cluster root@rabbit3 : rabbit@rabbit1
root@rabbit3 : rabbitmqctl start_app
root@rabbit3 : exit

$ docker exec -it rabbit2 bash
root@rabbit2 : /usr/sbin/rabbitmqctl stop_app
root@rabbit2 : rabbitmqctl join_cluster root@rabbit2 : rabbit@rabbit1
root@rabbit2 : rabbitmqctl start_app
root@rabbit2 : exit
~~~

### 3.3 미러링 구성하기
1번 도커 컨테이너로 접속 후 아래 명령어 실행
~~~bash
root@rabbit1 : rabbitmqctl -n rabbit1 set_policy act-policy "^cluster" '{"ha-mode":"all"}'
~~~

## 4. HA Proxy 구성
3개의 노드로 구성된 클러스터를 구성하였지만
클라이언트에서 해당 클러스터에 접근하기 위해서는 단일 진입점이 필요하다.
그리고 만약 3개의 노드 중 어떠한 노드가 내려간 경우 클라이언트에서 죽은 노드로 요청을 보내면 실패가 일어나기 때문에 Load Balance를 통해 health체크를 하기 위한 HA Proxy를 구성한다.

### 4.1 HA Proxy 설치 및 설정
설치
~~~bash
brew install haproxy
~~~

설정
/usr/local/etc/haproxy.cfg 파일을 아래와 같이 수정한다.
~~~
    listen rabbitmq_local_cluster
    bind *:5670
    mode tcp
    balance roundrobin
    server rabbit1 127.0.0.1:5672 check inter 5000 rise 2 fall 3
    server rabbit2 127.0.0.1:5673 check inter 5000 rise 2 fall 3
    server rabbit3 127.0.0.1:5674 check inter 5000 rise 2 fall 3
~~~

## 5. 클라이언트에서 접근
본 예제는 Spring boot 기준으로 작성 되어 있다.

### 5.1 기본 설정
amqp 라이브러리를 사용하기 위해 아래와 같이 dependency를 추가한다.
~~~xml
<dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
~~~

위에서 만들어 놓은 클러스터에 접근 하기 위해 haproxy정보를 application.yml에 입력한다.
~~~yml
spring:
  rabbitmq:
    host: localhost
    port: 5670
    username: guest
    password: guest
~~~

### 5.2 메시지 보내기
rest controller 소스는 아래와 같다.
~~~java
import coe.rabbitmq.coerabbitmq.api.Sender;
import coe.rabbitmq.coerabbitmq.dto.SendDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value ="/send", produces = MediaType.APPLICATION_JSON_VALUE)
public class SendController {

    @Autowired
    private Sender sender;

    @PostMapping
    public void setMessages (@RequestBody SendDTO sendDTO) {
        sender.send(sendDTO.getMessage());
    }
}
~~~

Sender 소스는 아래와 같다.

~~~java
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class Sender {
    RabbitMessagingTemplate rabbitMessagingTemplate;

    public Sender(RabbitMessagingTemplate rabbitMessagingTemplate) {
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
    }

    public void send(String message) {
      // test_cluster는 rabbitmq에 등록된 queue이름이다.
      rabbitMessagingTemplate.convertAndSend("cluster_test", message);
    }
}
~~~

해당 서버를 기동 시킨 이후 api 를 호출한다.

~~~bash
curl -d '{"message":"test"}' -H "Content-Type: application/json" -X POST http://localhost:20000/send
~~~

localhost:15672 queue에서 메시지를 확인한다.


### 5.3 1번 노드를 종료 시킨 이후 데이터 확인
1번 노드를 중지 한 이후에 2,3번 노드에 데이터가 제대로 존재하는지(미러링이 제대로 되었는지) 확인한다.

~~~bash
./rabbit1/sbin/rabbitmqctl -n rabbit1 stop_app
~~~

localhost:15673으로 접속 (15672는 종료 되었기 때문) 하여
Queue 메뉴에서 메시지가 존재하는지 확인한다.

HA Proxy가 제대로 동작 되는지를 확인하기 위해
아래와 같이 api 를 호출한다.

~~~bash
curl -d '{"message":"test2"}' -H "Content-Type: application/json" -X POST http://localhost:20000/send
~~~

Queue 메뉴에서 test2라는 메시지가 존재하는지 확인한다.


### 5.4 메시지 받기
아래와 같이 Receiver를 작성한다.

~~~java
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class Receiver {
    @RabbitListener(queues = "cluster_test")
    public void receiver(String message) {
        System.out.println(message);
    }
}
~~~

spring 서버를 재 기동 한 이후 메시지를 보내면 Receiver가 자동으로 읽어와서 콘솔에 찍는 것을 확인 할 수 있다.
