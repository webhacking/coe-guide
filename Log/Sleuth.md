# Spring Cloud Sleuth

Spring Cloud Sleuth는 Dapper, Zipkin, HTrace의 영향을 받아 만들어진 Spring Cloud를 위한 분산 추적 솔루션이다.
모든 interaction에 자동으로 적용된다.

![](../images/monitoring-log-global-transaction-id.png)
- Sleuth를 통해 Global Transaction id, Span id 부여

#### Span
>작업의 기본 단위이다. 예를 들어 RPC(Remote Procedure Call)을 전송하면 새로운 Span이 하나 생성된다.
Span은 64-bit ID로 구분하며 Span 여러 개를 포함하는 Trace도 마찬가지로 64-bit ID를 갖는다.
Span은 또한 Description, key-value annotation, process ID 등의 다른 데이터도 가질 수 있다.
Span은 시작, 종료 시점에 대한 timing 정보를 갖고 있다.   

#### Trace
>Tree 형태로 만들어지는 Span의 모음이다.
예를 들어 분산 데이터 스토어를 운영할때, 하나의 Put 리퀘스트에 의해 하나의 Trace가 생성된다.

#### Spring Cloud Sleuth의 기능

- Slf4J MDC(Mapped Diagnostic Context)을 통해 Trace와 Span Ids를 추가할 수 있다. 이를 통해 로그로 부터 트레이스와 Span 정보들을 로그 수집기에 추출할 수 있다.
- Sleuth는 분산 추적 데이터 모델(Trace, Spans, Annotations, k-v Annotation)을 제공한다.
  Zipkin에 최적화 되어 있으며, HTrace에 적용 가능하다.

- 스프링 어플리케이션의 공통 진출입점에 적용 가능하다.
(servlet filter, rest template, scheduled actions, message channels, zuul filters, feign client)

#### 설치방법
- pom.xml 에 dependency 추가
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-sleuth</artifactId>
            <version>1.3.4.BUILD-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-sleuth</artifactId>
    </dependency>
</dependencies><repositories>
    <repository>
        <id>spring-snapshots</id>
        <name>Spring Snapshots</name>
        <url>https://repo.spring.io/libs-snapshot</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

- log 추가 예제 코드 (별도 작업 필요 없음)
```java
@SpringBootApplication
@RestController
public class Application {

  private static Logger log = LoggerFactory.getLogger(DemoController.class);

  @RequestMapping("/")
  public String home() {
    log.info("Handling home");
    return "Hello World";
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
```

- 로그 예제
```text
2018-05-11 15:21:46.466  INFO [sleuth-example,8f8964f0eebf01a8,8f8964f0eebf01a8,false     ] 2647 --- [nio-8080-exec-1] com.example.slueth.SleuthApplication     : This is your home!
                              [appname       ,traceId         ,spanId          ,exportable]
```

- Logback.xml 추가 설정 예제
```xml
%d{yyyy-MM-dd} HH:mm:ss.SSS} %5p [$APP_NAME:-],%X{X-B3-TraceId:-},%
```


> zipkin vs. jaeger 참고
<img src='../images/zipkinVsJaeger.png'>
[출처](https://sematext.com/blog/jaeger-vs-zipkin-opentracing-distributed-tracers/)
