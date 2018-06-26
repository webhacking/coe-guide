# 1. 개요
- Pinpoint는 분산 시스템 환경에서 log tracing/request tracing을 해주는 도구 이다.  
- bytecode instrumentation 방식 으로 소스코드 수정이 불필요 하다.
(클래스를 로드하는 시점에 코드를 가로채 바이트코드를 변경)
- 사용성 분석 관련 Google Analytics로 데이터를 전송하는데, pinpoint-web.propertis에 아래 속성 추가 필요
```xml
config.sendUsage=false
```
- pinpoint 사용시 기존 resource 사용량에 3% 정도의 추가 resource 사용량이 필요 하다고 함(과연 그럴지...)

### Architecture
Agent(서비스단에 추가되는 bytecode), Collector, HBase, UI Web 로 구성 됨.
![](../images/pinpoint-architecture.png)  
[출처: Naver Pinpoint](http://naver.github.io/pinpoint/quickstart.html#extra)  

### 주요 기능
#### callstack monitoring
![](../images/pinpoint-callstack.png)  
[출처: Naver Pinpoint](http://naver.github.io/pinpoint/quickstart.html#extra)  
#### 서비스 성능 inspector
![](../images/pinpoint-inspector.png)  
[출처: Naver Pinpoint](http://naver.github.io/pinpoint/quickstart.html#extra)  
#### request tracking
![](../images/pinpoint-request-scatter-chart.png)  
[출처: Naver Pinpoint](http://naver.github.io/pinpoint/quickstart.html#extra)  

# 2. Quick Start
소스코드 다운로드 [Naver Pinpoint]('https://github.com/naver/pinpoint.git') 후 바로 서비스를 시작 할 수 있다.
- collector의 ip와 port, log 경로 및 hbase 정보 변경은 quickstart/collector/src/main/resources에서 가능하다.
- web 의 port 변경은 quickstart/conf의 파일에서 가능하다.

서비스 시작  
1. quickstart/bin/start-hbase.sh  quickstart/bin/init-hbase.sh (db 초기화)
2. quickstart/bin/start-collector.sh  
3. quickstart/bin/start-web.sh  
4. 사용자의 서비스 시작 시 아래 agent정보를 추가하여 실행한다.
```sh
-javaagent:/Users/actmember/workspace/msa-study/pinpoint/quickstart/agent/target/pinpoint-agent/pinpoint-bootstrap-1.8.0-SNAPSHOT.jar  
-Dpinpoint.agentId=service01  
-Dpinpoint.applicationName=service01  
```
5. http://localhost:28080로 접속하여 서비스 이용 가능

####. UI에서 Applicatinos 간 관계 설정
Application 서비스 간의 관계를 별도로 설정하지 않는다.
HBase에 적재되는 데이터를 pinpoint가 읽어서 관계를 자동 구성한다.

# 3. Manual Build 하기
[가이드](http://naver.github.io/pinpoint/installation.html#5-pinpoint-agent)에 나와있는데로, JDK환경 변수를 모두 구성 후 빌드를 한다.
- 빌드 후 결과 파일은 collector/ agent / web 폴더의 target에 존재한다.
- agent의 경우 target에 생성되는 압축파일을 가져다 사용한다. 압축을 해제하면 jar 파일및 관련 파일들이 포함되어 있다. 여기서 conf 를 꼭 다시 확인해야한다.
- 압축해제 경로의 pinpoint-agent.config 파일에서 collector정보를 수정해 줘야 한다.

# 참고
작동원리 : https://d2.naver.com/helloworld/1194202
