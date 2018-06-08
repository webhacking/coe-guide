# Fluentd

## [installation](https://docs.fluentd.org/v0.12/articles/install-by-docker)

### Installation Test
1. create fluentd.conf to ./fluentd
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
1. install & run fluentd with docker
    ```bash
    $ docker pull fluent/fluentd:v0.12-debian
    $ docker run -d -p 9880:9880 -v $PWD/fluentd:/fluentd/etc -e FLUENTD_CONF=fluentd.conf fluent/fluentd
    ```
3. test
    > curl -X POST -d 'json={"json":"message"}' http://localhost:9880/sample.test
4. result
    > 2017-01-30 14:04:37 +0000 sample.test: {"json":"message"}

### Log to local file
1. set fluentd.conf
    ```xml
    ## INPUT
    <source>
    @type tail        ## 파일의 내용 감시 패턴 타입 지정
    format none       ## 파일 포멧 타입 지정 안함
    path /fluentd/etc/log/*.log ## 로그위치
    pos_file /fluentd/etc/log/order-service.pos ## 정합성을 위한 포지션 저장 파일
    tag order-service ## 식별자 지정
    </source>

    ## OUTPUT
    <match **>
    @type file  ## 파일로 output 저장
    path /fluentd/log/order-service.log ## 저장 파일명으로 time_slice_format과 결합하여 파일 생성 (ex /fluentd/log/order-service.log.201806081550)
    time_slice_format %Y%m%d%H%M
    time_slice_wait 10m   ## 임시파일이 실제 파일명으로 변경되기까지의 소요시간
    time_format %Y%m%d%T%H%M%S%z 
    </match>
    ```
2. restart docker instance(or create new instance)
    ```bash
    $ docker restart {fluent_instance}
    ```
3. result
    ```bash
    $ docker exec -t {fluent_instance} ls /fluentd/log
    order-service.log                     order-service.log.201806080653_0.log
    order-service.log.201806080651_0.log
    $ docker exec -t {fluent_instance} cat /fluentd/log/order-service.log.201806080651_0.log
    2018-06-08T06:51:07+00:00	fluent.info	{"worker":0,"message":"fluentd worker is now running worker=0"}
    2018-06-08T06:51:15+00:00	order-service	{"message":"2018-06-08 15:51:06.480 [http-nio-17003-exec-8] [fd660dd005aae5de, 3fbb812ba2b7ca34] INFO  o.c.c.web.rest.OrderController - hi"}
    2018-06-08T06:51:15+00:00	order-service	{"message":"2018-06-08 15:51:07.157 [http-nio-17003-exec-9] [3decc190566831ac, a1f9844c573cae20] INFO  o.c.c.web.rest.OrderController - hi"}
    2018-06-08T06:51:18+00:00	order-service	{"message":"2018-06-08 15:51:10.138 [AsyncResolver-bootstrap-executor-0] [, ] INFO  c.n.d.s.r.aws.ConfigClusterResolver - Resolving eureka endpoints via configuration"}
    2018-06-08T06:51:22+00:00	order-service	{"message":"2018-06-08 15:51:14.161 [http-nio-17003-exec-1] [707d59eb57ca72b3, 281ab827996639cf] INFO  o.c.c.web.rest.OrderController - hi"}
    ```

### Log to remove server

# 참고
- [EFK 구성](http://louky0714.tistory.com/entry/BigData-EFK%EC%84%A4%EC%B9%98%EC%9E%91%EC%84%B1%EC%A4%91-1)
