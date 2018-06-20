# Fluentd

### 설치
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

### Spring Boot 어플리케이션 로그 수집
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

### 중앙 집중식 로그관리 
1. client agent의 fluentd.conf 설정
2. fluentd 서버 (도커 / config)
3. 테스트

### EFK
1. Elasticsearch에 로그 저장
...

# 참고
- [EFK with Docker Compose](https://docs.fluentd.org/v0.12/articles/docker-logging-efk-compose)
