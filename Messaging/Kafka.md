# Kafka

### Why Kafka?
- decoupling of data streams (middle-layer)
- distributed, resilient architecture, fault tolerant
- horizontal scalability

### What is Kafka?
- Messaging System : publish and subscribe to streams of records
- Store System : store streams of records in a fault-tolerant durable way
- Stream Processing : Process streams of records as they occur

- messaging system
- activity tracking
- gather metrics
- logs gathering
- stream processing
- decoupling of system dependencies

sourcesystem -> producers -> kafka -> consumer -> targetsystem
                             (zookeeper)
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


### Topics and partitions
- topic : particular stream of data (similar to table in a database)
- partitions : topics are split in partitions
	       each partition is ordered (파티션 내부에서만 순서 보장됨)
	       파티션 내에서 각 메세지에 대한 incremental id(offset)를 갖음
	       default 일주일동안 보관됨. 이후 삭제됨. 
	       데이터가 파티션에 쓰여지면 immutability
	       데이터는 파티션에 랜덤하게 쓰여지지만, 동일한 key를 가지는 데이터는 동일한 파티션에 쓰여진다
	       토픽 당 many partitions 가질 수 있다(파티션 많을수록 parallelism 증가)
### Brokers
- kafka cluster는 multiple brokers(server)로 구성된다.
- 각 브로커는 unique id로 구분된다
- 특정 브로커에 연결되면, 전체 클러스터에 있는 브로커 전체에 연결된다 (모든 데이터는 각 broker에 분산돼 있지만 하나에 연결시 전체로 연결되므로 문제없음)
- 3 brokers로 시작하는 것이 좋다

### Topic replication factor
- topic은 replication factor > 1 (usually 2-3(optimal)) 

### Leader for partition
at any time only one broker can be a leader for a given partition 
only that leader can receive and serve data for partition 

하나의 브로커는 특정 파티션의 리더가 되고 리더만 데이터를 받고 처리할 수 있다. 
다른 브로커들은(in-sync replica)는 빠른 속도로 리더로부터 데이터를 동기화한다. 

각 파티션은 하나의 리더와 다수의 ISR(rf - 1)을 갖는다.

### Producers
- write data to topics
- topic name을 가지고 하나의 브로커에 메세지를 발행하면, 카프카가 자동으로 데이터가 저장될 브로커에 라우팅을 한다
- 데이터는 자동으로 로드밸렁신 돼 partition에 랜덤하게 분산된다 
- Producer는 브로커 서버로부터 응답(acknowledgment of data writes) 단계를 선택할 수 있다
   - Acks=0 : producer는 acknowledgement를 기다리지 않음 (손실 위험 있으나 매우 빠름) ex) log
   - Acks=1 : leader acknowledgment를 기다림 (limited data loss)
   - Acks=all :  Leader + replicas acknowledgment (no loss) ex) transactional data
- message keys를 포함해서 발행하면 동일한 파티션으로 전달 돼 순서가 보장됨

### Consumers
- read data from a topic
- consumer는 특정한 topic name을 가지고 데이터를 pull
- 하나의 특정 브로커에 커넥트 시, 데이터가 있는 브로커에서 자동으로 데이터를 가져옴
- 파티션 간에는 parallel하게 read하나 파티션 내에선 순서가 보장됨

### Consumer groups
- 그룹 내에 각 컨슈머는 서로 다른 파티션의 데이터를 읽어온다 -> 파티션 수 보다 컨슈머 수가 많으면 inactive 

### Consumer offsets
- kafka는 컨슈머 그룹이 읽어간 offset정보를 저장함
- __consumer_offsets 라는 카프카 토픽에 저장된다
- 컨슈머가 카프카에서 받은 데이터를 처리하고 offsets을 커밋함
- 만약 컨슈머가 죽으면 해당 offset부터 다시 읽어올 수 있음   
- 컨슈머가 5분동안 응답이 없으면 백업을 하고. 카프카는 컨슈머한테 가져가지 않은 offset 정보를 알려준다 

### Zookeepr
- manages broker
- zookeeper없이 동작하지 
- manages broker않음  
- leader election for partitions
- send notifications to kafka in case of changes( new topic, broker dies..........)
- zookeeper 홀수로 구성 (odd quorum cluster of servers 3, 5, 7...)
- leader + followers (kafka 브로커들은 leader에만 접속)
- leader election + all broker들이 configuration 공유하게 해줌

### kafka guarantees
- 메세지는 topic-partition에 시간순으로 저장되고 컨슈머는 시간순으로 읽어간다
- Replication factor가 N 이면 N-1개의 브로커가 다운되는 것까지 견딜 수 있다 
- 하나의 브로커는 maintenance로 떨어질 수 있기 때문에 적어도 RF는 3개가 좋다
- 토픽에 파티션수가 동일하게 유지되는한, 동일한 키를 가진 메세지는 동일한 파티션에 저장된다
  (운영 중 파티션 수를 늘이거나 줄이면 보장되지 않으므로 초기 설계 시 고려필요)
  
  
### Delivery semantics for consumers
- 컨슈머가 offsets을 커밋할 때 레벨을 선택할 수 있음
  - at most once : 메세지를 받자마자 커밋. 받고 컨슈가 처리 중 죽으면 메세지 손실될 수 있음(it wont be read again)
  - (bottom line) at least once : 메세지를 받고 프로세스 처리 된 후에 커밋. data duplication. idempotent(다시 받아도 시스템에 영향 주지 않는 시스템)
  - exactly once : very difficult to achieve 
  
### Topic Configuration
```bash
kafka-topics --create --topic test_cleanup --zookeeper localhost:2181 --config cleanup.policy=compact --partitions 3 --replication-factor 1

kafka-topics --topic test_cleanup --describe --zookeeper localhost:2181

kafka-topics --alter --topic test_cleanup --zookeeper localhost:2181 --config cleanup.policy=delete
```

##### Partitions Count, Replication Factor
- impact performance and durability of system overall
- 처음 토픽 생성할 때 최적으로 만들어야함
	- 파티션 수 증가하면 key ordering 보장안됨
	- RF 증가하면, 클러스터에 부하를 줌. Performance decrease. 디스크 차지 증가

Partitions Count 
- each partition 약 10MB/s 처리
- more partition = better parallelism, betther throughput
                   but, files opened 많아짐
		   but, 하나의 브로커가 비정상적으로 내려가면 leader election
		   but, latency to replicate
- Guideline : # of partition per brokers 2000~4000
              # of partitions in the cluster < 20000
	      ***Partitions per topic = (1 to 2) x (# of brokers), max 10 partitions***
	      ex) 3brokers. 3 or 6 partitions is good
	      
Replication Factor
- at least 2, maximum of 3
- higher RF = better resilience (N-1 can fail)
              but, longer replication (higher latency is acks=all)
	      but, more disk space
- Guideline : 3 (at least 3 brokers for that)
	      
#### Partitions and Segments
- partitions are made of segments(files)
 

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
