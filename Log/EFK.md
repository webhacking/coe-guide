# 1. 개요

## 목적

### 로그 관리의 어려움

![](../images/log-overview-current-issue.png)

각 마이크로서비스는 독립적인 물리적 장비나 가상머신에서 운영된다.  
만약 각자의 로컬 파일 시스템에 로그를 남긴다면 여러 서비스에 걸쳐서 발생하는 트랜잭션의 흐름을 처음부터 끝까지 연결해가며 이해하는 것이 매우 어렵고 서비스 수준에서 로그를 통합하고 집계하는 것은 거의 불가능하다.

### 중앙 집중형 로깅
위와 같은 문제들을 해결하려면 로그의 출처와 관계없이 모든 로그를 중앙 집중적으로 저장하고 분석해야 한다.  
이를 위해선 로그의 저장과 처리를 서비스 실행 환경에서 떼어 내야 한다.  
각 서비스에서 발생한 로그들은 한곳에 모아진 후 중앙의 빅데이터 저장소로 보내지고, 빅데이터 솔루션을 이용해 로그를 분석하고 처리한다.  

![](../images/log-overview-aggregation.png)

- 로그 스트림(log stream) : 로그 생성자가 만들어내는 로그 메시지의 스트림. (일반적인 자바 기반 시스템에서 Log4j 로그스트림)
- 로그 적재기(log shipper) : 메시지를 데이터베이스에 쓰거나, 대시보드에 푸시, 스트림 처리 종단점으로 보내는 등 여러 다른 종단점으로 메세지를 전송  
  ex) Logstash, Fluentd
- 로그 저장소(log store) : 로그 메시지 저장을 위한 대용량 데이터 저장소  
  ex) HDFS, Elasticsearch, NoSQL
- 로그 대시보드 : 로그 분석 결과를 시각화.  
  ex) Kibana, Graphite

### 커스텀 로깅 솔루션
커스텀 로깅 솔루션을 구축할 때 가장 많이 쓰이는 컴포넌트의 조합은 *ELK(Elasticsearch, Logstash, Kibana)* 또는 *EFK(Elasticsearch, Fluentd, Kibana)* 스택이다.
![](../images/log-overview-EFK.png)

- 로그 적재기 : 가장 많이 사용되는 Logstash나 Fluentd는 로그 파일을 수집하고 적재하는 데 사용할 수 있는 강력한 데이터 파이프라인 도구이다. 서로 다른 소스에서 스트리밍 데이터를 받아 다른 대상과 동기화하는 매커니즘을 제공하는 브로커 역할을 한다. [참고: comparing fluentd and logstash](https://www.alibabacloud.com/help/doc-detail/44259.html?spm=a2c5t.11065259.1996646101.searchclickresult.4687619dZP3Baj)  
각 서비스에 있는 Fluentd에서 저장소로 바로 전달하지 않고 중간에 Fluentd를 넣는 이유는 Fluentd가 앞에서 들어오는 로그들을 수집하고 저장소에 넣기 전 트래픽을 Throttling해서 로그 저장소의 용량에 맞게 트래픽을 조정 할 수 있다. 또는 로그의 종류에 따라서 각각 다른 로그 저장소로 라우팅하거나 여러개의 저장소에 저장할 수 있다.
- 로그 저장소 : 실시간 로그 메시지는 일반적으로 Elasticsearch에 저장된다. Elasticsearch를 사용하면 클라이언트가 텍스트 기반 인덱스를 바탕으로 쿼리할 수 있다. 이 외에도 HDFS는 일반적으로 아카이브된 로그 메시지를 저장하는 데 사용된다. MongoDB나 Cassandra는 매월 집계되는 트랜잭션 수와 같은 요약 데이터를 저장하는데 사용된다.
- 대시보드 : 로그 분석을 위해 가장 일반적으로 사용되는 대시보드는 Elasticsearch 데이터 스토어를 기반으로 사용되는 Kibana(키바나)가 있다. Graphite(그래파이트) 및 Grafana(Grafana)도 로그 분석 보고서를 표시하는데 사용된다.


##### Kibana Dashboard
![](../images/efk-kibana-console.png)




# 2. Fluentd 설치 및 테스트

전체 EFK 스택을 구성하기 전 간단한 Fluentd 사용 방법 및 로그파일 입출력을 테스트 해봅니다.
#### Fluentd 설치 및 테스트
- [install-by-docker](https://docs.fluentd.org/v0.12/articles/install-by-docker)

1. ./fluentd 폴더에 fluentd.conf 파일 추가
    ```xml
    <source>
    @type http
    port 9880
    bind 0.0.0.0
    </source>
    <match **>
    @type stdout
    </match>
    ```
1. docker를 이용한 설치 및 구동
    ```bash
    $ docker pull fluent/fluentd:v0.12-debian
    $ docker run -d -p 9880:9880 -v $PWD/fluentd:/fluentd/etc -e FLUENTD_CONF=fluentd.conf fluent/fluentd
    ```
3. 테스트
    ```bash
    $ curl -X POST -d 'json={"json":"message"}' http://localhost:9880/sample.test
    $ docker ps
    CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                                         NAMES
    b495e527850c        fluent/fluentd      "/bin/sh -c 'exec ..."   2 hours ago         Up 2 hours          5140/tcp, 24224/tcp, 0.0.0.0:9880->9880/tcp   awesome_mcnulty
    $ docker logs b49 | tail -n 1
    ```
    > 2017-01-30 14:04:37 +0000 sample.test: {"json":"message"}

#### Spring Boot 어플리케이션 로그 수집 테스트
1. Spring Boot 어플리케이션에 logback-spring.xml 추가 (/src/main/resources/)
    ```xml
    ...
        <include resource="org/springframework/boot/logging/logback/base.xml"/>
        <springProperty scope="context" name="springAppName" source="spring.application.name"/>
        <property name="LOG_FILE"
                value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}/spring_%d{yyyy-MM-dd}.log}"/>
        <property name="LOG_PATH" value="${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}"/>
        <property name="COE_FILE_LOG_PATTERN"
                value="%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%15.15t] %-40.40logger{39} [%4.4L] : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"/>

        <appender name="dailyRollingFileAppender" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>${LOG_PATH}/application_${springAppName}_%d{yyyy-MM-dd}.log</fileNamePattern>
                <!--Log files older than 30 days are deleted-->
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
                <level>INFO</level>
            </filter>
            <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
                <layout class="ch.qos.logback.classic.PatternLayout">
                    <pattern>${COE_FILE_LOG_PATTERN}</pattern>
                </layout>
            </encoder>
        </appender>
    ...
    ```
2. fluentd.conf 설정
    ```xml
    ## INPUT
    <source>
    @type tail                                  ## 파일의 내용 감시 패턴 타입 지정
    format none                                 ## 파일 포멧 타입 지정 안함
    path /fluentd/etc/log/*.log                 ## 로그위치
    pos_file /fluentd/etc/log/order-service.pos ## 정합성을 위한 포지션 저장 파일
    tag order-service                           ## 식별자 지정
    </source>

    ## OUTPUT
    <match **>
    @type file                                  ## 파일로 output 저장
    path /fluentd/log/order-service.log         ## 저장 파일명으로 time_slice_format과 결합하여 파일 생성
                                                ## (ex /fluentd/log/order-service.log.201806081550)
    time_slice_format %Y%m%d%H%M
    time_slice_wait 10m                         ## 임시파일이 실제 파일명으로 변경되기까지의 소요시간
    time_format %Y%m%d%T%H%M%S%z
    </match>
    ```

2. docker 컨테이너 재시작
    ```bash
    $ docker restart {fluent_container_id}
    ```
3. 테스트 결과
    ```bash
    $ docker exec -t {fluent_container_id} ls /fluentd/log
    order-service.log                     order-service.log.201806080653_0.log
    order-service.log.201806080651_0.log
    $ docker exec -t {fluent_container_id} cat /fluentd/log/order-service.log.201806080651_0.log
    2018-06-08T06:51:07+00:00	fluent.info	{"worker":0,"message":"fluentd worker is now running worker=0"}
    2018-06-08T06:51:15+00:00	order-service	{"message":"2018-06-08 15:51:06.480 [http-nio-17003-exec-8] [fd660dd005aae5de, 3fbb812ba2b7ca34] INFO  o.c.c.web.rest.OrderController - hi"}
    2018-06-08T06:51:15+00:00	order-service	{"message":"2018-06-08 15:51:07.157 [http-nio-17003-exec-9] [3decc190566831ac, a1f9844c573cae20] INFO  o.c.c.web.rest.OrderController - hi"}
    2018-06-08T06:51:18+00:00	order-service	{"message":"2018-06-08 15:51:10.138 [AsyncResolver-bootstrap-executor-0] [, ] INFO  c.n.d.s.r.aws.ConfigClusterResolver - Resolving eureka endpoints via configuration"}
    2018-06-08T06:51:22+00:00	order-service	{"message":"2018-06-08 15:51:14.161 [http-nio-17003-exec-1] [707d59eb57ca72b3, 281ab827996639cf] INFO  o.c.c.web.rest.OrderController - hi"}
    ```

# 3. EFK 스택 구성 방법

도커를 이용하여 전체 EFK 스택을 구성해 봅니다.

### 컴포넌트 구성
![](../images/log-overview-EFK-sample.png)
1. order-service
2. fluentd for each service
3. fluentd for aggregator
4. elasticsearch
5. kibana

### 1. Spring Boot 어플리케이션에 logback-spring.xml 추가 (/src/main/resources/)
```xml
...
    <include resource="org/springframework/boot/logging/logback/base.xml"/>
    <springProperty scope="context" name="springAppName" source="spring.application.name"/>
    <property name="LOG_FILE"
            value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}/spring_%d{yyyy-MM-dd}.log}"/>
    <property name="LOG_PATH" value="${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}"/>
    <property name="COE_FILE_LOG_PATTERN"
            value="%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%15.15t] %-40.40logger{39} [%4.4L] : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"/>

    <appender name="dailyRollingFileAppender" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/application_${springAppName}_%d{yyyy-MM-dd}.log</fileNamePattern>
            <!--Log files older than 30 days are deleted-->
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>INFO</level>
        </filter>
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="ch.qos.logback.classic.PatternLayout">
                <pattern>${COE_FILE_LOG_PATTERN}</pattern>
            </layout>
        </encoder>
    </appender>
...
```

### 2. fluent(client, aggregator), elasticsearch, kibana는 도커를 이용해 구성한다
  docker-compose를 이용하여 4개의 컨테이너를 실행시킨다.
  - fluent-client : 서비스의 로그파일을 읽어 중앙에 있는 fluentd-aggregator에게 전송한다.
  - fluent-aggregator : 각 fluent-client로부터 전달 된 로그들을 elasticsearch로 포워딩한다.
  - elasticsearch : 로그 저장소
  - kibana : elasticsearch에 저장돼 있는 로그들을 시각화한다.

  실행을 위한 로컬 파일 목록
  ```text
  Step-a
  ./docker-compose.xml
  Step-b
  ./fluentd-client/Dockerfile
  ./fluentd-client/conf/fluentd.conf
  Step-c
  ./fluentd-aggregator/Dockerfile
  ./fluentd-aggregator/conf/fluentd.conf
  ```

  a. ./docker-compose.yml 작성
  ```yaml
  version: '2'
services:
  fluentd-client:
    build: ./fluentd-client
    volumes:
      - ./fluentd-client/conf:/fluentd/etc    # 로컬의 fluentd.conf 파일을 컨테이너로 마운트  
      - /Users/boston/Developer/log:/var/log  # 로컬에 쌓인 서비스의 로그파일을 컨테이너로 마운트
    links:
      - "fluentd-aggregator"
    ports:
      - "25000:25000"

  fluentd-aggregator:
    build: ./fluentd-aggregator
    volumes:
      - ./fluentd-aggregator/conf:/fluentd/etc # 로컬의 fluentd.conf 파일을 컨테이너로 마운트
    links:
      - "elasticsearch"
    ports:
      - "24224:24224"
      - "24224:24224/udp"

  elasticsearch:
    image: elasticsearch
    expose:
      - 9200
    ports:
      - "9200:9200"

  kibana:
    image: kibana
    links:
      - "elasticsearch"
    ports:
      - "5601:5601"
  ```

 b. fluentd-client 도커파일 및 fluentd config 파일 생성

  도커파일 생성

  ```bash
  > mkdir fluentd-client
  > cd fluentd-client
  > vi Dockerfile
  ```
  ```bash
  # fluent-client/Dockerfile
  FROM fluent/fluentd
  RUN ["gem", "install", "fluent-plugin-grok-parser", "-v 2.1.4"]
  ```
    - grok-parser : 로그파일을 항목별로 파싱하기 위한 라이브러리 (fluent.conf의 parser 영역에 패턴을 지정한다)

  fluentd config 파일 생성

  ```bash
  > mkdir conf
  > cd conf
  > vi fluent.conf
  ```

  ```text
  # fluent-client/conf/fluent.conf
  <source>
        @type tail
        format none
        path /var/log/*.log
        pos_file /var/log/order-service.pos
        tag order-service
        <parse>
                @type multiline_grok
                grok_pattern %{TIMESTAMP_ISO8601:timestamp}\s+%{LOGLEVEL:severity}\s+\[%{DATA:service},%{DATA:trace},%{DATA:span},%{DATA:exportable}\]\s+%{DATA:pid}\s+---\s+\[\s*%{DATA:thread}\]\s+%{DATA:class}\s+\[\s*%{NUMBER:line}\]\s:\s+%{GREEDYDATA:rest}
    multiline_start_regexp /^[\d]/
        </parse>
</source>
<match order-service>
        @type forward
        <server>
                name fluentd-aggregator
                host fluentd-aggregator
                port 24224
        </server>
</match>
<match order-service>
        @type stdout
</match>
```
    - source : log파일을 읽어와(@type tail) grok을 통해 파싱한다(@type multiline_grok).
    - match : source에서 읽어온 데이터를 fluentd-aggregator에 포워딩한다(@type forward).

  c. fluentd-aggregator 도커파일 및 fluentd config 파일 생성
  도커파일 생성

  ```bash
  > mkdir fluentd-aggregator
  > cd fluentd-aggregator
  > vi Dockerfile
  ```
  ```bash
  # fluent-aggregator/Dockerfile
  FROM fluent/fluentd
  RUN ["gem", "install", "fluent-plugin-elasticsearch", "--no-rdoc", "--no-ri", "--version", "1.9.2"]
  ```
    - fluent-plugin-elasticsearch : elasticsearch로 전송하기 위한 라이브러리  

  fluentd config 파일 생성

  ```bash
  > mkdir conf
  > cd conf
  > vi fluent.conf
  ```

  ```text
  # fluent-aggregator/conf/fluent.conf
  <source>
      @type forward
      port 24224
      bind 0.0.0.0
  </source>
  <match *.**>
      @type copy
      <store>
        @type elasticsearch
        host elasticsearch
        port 9200
        logstash_format true
        logstash_prefix fluentd     # elasticsearch index 생성 정보(미입력시 logstash)
        logstash_dateformat %Y%m%d  # fluentd-20180101 형식으로 index 생성
        include_tag_key true
        type_name access_log
        tag_key @log_name
        flush_interval 1s
      </store>
  </match>
  ```
    - source : fluentd-client에서 포워딩한 데이터
    - match : source에서 읽어온 데이터를 elasticsearch로 전송한다(@type elasticsearch).

### 3. Docker 컨테이너 실행
  ```bash
  > docker-compose up
  ```
  정상 작동 시 아래와 같이 4개의 컨테이너가 구동된다.  
  ![](../images/efk-stack-docker.png)

### 4. Kibana 확인
localhost:5601로 접속하여 kibana 작동 확인

a. Index Pattern 설정

![](../images/efk-kibana-setting1.png)    

![](../images/efk-kibana-setting2.png)
  - index pattern : elasticsearch 인덱스 패턴
  - Time Filter field name : Time 필드가 있을 경우 설정

b. 로그 확인  
 ![](../images/efk-kibana-console.png)  
  - Selected Field와 Available Field를 Add/Remove 하여 컬럼에 노출될 데이터 및 순서를 변경할 수 있다.






# 참고
- [EFK with Docker Compose](https://docs.fluentd.org/v0.12/articles/docker-logging-efk-compose)
