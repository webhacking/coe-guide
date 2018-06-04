# Kafka

### What is Kafka?
- Messaging System : publish and subscribe to streams of records
- Store System : store streams of records in a fault-tolerant durable way
- Stream Processing : Process streams of records as they occur

### Design Motivation
Kafka는 real-time 데이터를 처리하기 위한 통합된 플랫폼을 제공하기 위해 만들어졌다. 
이를 위해선,
 - 실시간 로그 수집 같은 대용량 이벤트 스트림을 지원하기 위한 높은 처리량
 - 오프라인 시스템으로부터 주기적인 데이터 로드를 위해 큰 규모의 데이터 백로그를 다룸
 - 전통적인 메세징 처리를 다루기 위한 Low-latency 전달
 - Partitioned, Distributed, Real-time processing
 - Fault-tolerance 
 
 ### Feature
 - Kafka는 메세지를 파일시스템에 저장하므로 데이터의 영속성이 보장된다. "디스크 IO는 느리다"는 인식이 있지만 잘 설계된 디스크 구조는 네트워크처리 만큼 빠르다. 
 - Consumer는 Pull 방식으로 데이터를 가져온다.
 	- Consumer의 처리능력만큼의 데이터를 가져올 수 있음

### Kafka 구동 방법

서버 시작
```sh
주키퍼 시작
bin/zookeeper-server-start.sh -daemon config/zookeeper.properties
카프카 시작
bin/kafka-server-start.sh -daemon config/server.properties
```

Topic 생성
```sh
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic_name
```

Topic 리스트 확인
```sh
bin/kafka-topics.sh --list --zookeeper localhost:2181
```

Producer (메세지 보내기)
```sh
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topic_name
> Hello World
```

Consumer (메세지 가져오기)
```sh
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic topic_name --from-beginning
> Hello World
```

### Kafka - Spring Clound Stream
pom.xml에 Kafka dependency 추가
```xml
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-stream</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-stream-kafka</artifactId>
</dependency>
```
property 추가
- 미설정 시 default 자동 설정

	- localhost:9092
	- zookeeper : localhost:2181
```xml
spring.cloud.stream.kafka.binder.brokers=localhost
spring.cloud.stream.kafka.binder.defaultBrokerPort=9092
```
producer 샘플
```java
public interface KafkaCustomSource {

    String TEST_SOURCE =  "testSource"; // topic name

    @Output
    MessageChannel testSource();

}
```
```java
@EnableBinding(KafkaCustomSource.class)
public class KafkaProducerService {

    private KafkaCustomSource source;

    public KafkaProducerService(KafkaCustomSource source) {
        this.source = source;
    }

    @SendTo(KafkaCustomSource.TEST_SOURCE)
    public String send() {
        return "Hello World";
    }
}
```
https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#_apache_kafka_binder

http://epicdevs.com/17
