# Docker 없이 EFK 설치하기

## Port 정보
elasticsearch 9200
fluentd-aggregator 24224 
kibana 5601

## Fluentd
#### Before Installing
- Setup NTP
logging 시간 동기화를 위함
- Increase Max # of File Desc
ulimit -n 명령어를 값을 확인할수 있고,
아래 내용을 /etc/security/limits.conf에 추가 후 재시작  

  ```conf
  root soft nofile 65536
  root hard nofile 65536
  * soft nofile 65536
  * hard nofile 65536
  ```
- Optimize Network Kernel Parameters
여러 Fluentd instan를 사용하는 경우 최적의 성능을 위해,
아래 내용을 /etc/sysctl.conf에 추가 후 재시작
```conf
net.core.somaxconn = 1024
net.core.netdev_max_backlog = 5000
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_wmem = 4096 12582912 16777216
net.ipv4.tcp_rmem = 4096 12582912 16777216
net.ipv4.tcp_max_syn_backlog = 8096
net.ipv4.tcp_slow_start_after_idle = 0
net.ipv4.tcp_tw_reuse = 1
net.ipv4.ip_local_port_range = 10240 65535
```
[출처](https://docs.fluentd.org/v1.0/articles/before-install)

#### 사내망이라 yum 설치가 안되는 경우
해당 패키지의 repository 정보를 사내 Nexus에 추가할 수 있음  
Jira의 DevOps_Support 프로젝트에서 issue로 등록 함  
> 추가 방법 상세 안내는 사내 Confluence DevOps Support 공간에 Guide > Library Repo. Guide 참고  

#### Installing Fluentd
curl로 설치 스크립트를 다운받아 실행 함.  
> GPG key는 txt복사 후 파일로 생성해도 됨  
> 이 경우 repo에는 file:///filepath 로 변경 해야 함  

```
$ curl -L https://toolbelt.treasuredata.com/sh/install-redhat-td-agent3.sh | sh
```
```sh
echo "=============================="
echo " td-agent Installation Script "
echo "=============================="
echo "This script requires superuser access to install rpm packages."
echo "You will be prompted for your password by sudo."

# clear any previous sudo permission
sudo -k

# run inside sudo
sudo sh <<SCRIPT

  # add GPG key
  rpm --import https://packages.treasuredata.com/GPG-KEY-td-agent

  # add treasure data repository to yum
  cat >/etc/yum.repos.d/td.repo <<'EOF';
[treasuredata]
name=TreasureData
baseurl=http://packages.treasuredata.com/3/redhat/\$releasever/\$basearch
gpgcheck=1
gpgkey=https://packages.treasuredata.com/GPG-KEY-td-agent
EOF

  # update your sources
  yum check-update

  # install the toolbelt
  yes | yum install -y td-agent

SCRIPT

# message
echo ""
echo "Installation completed. Happy Logging!"
echo ""
```
Launch Daemon  

- systemd  
If you want to customize systemd behaviour, put your td-agent.service into /etc/systemd/system

  ```sh

  $ sudo systemctl start td-agent.service
  $ sudo systemctl status td-agent.service
  $ sudo systemctl enable td-agent.service  # 서버 시작시 기동 됨
  ```
- init.d
Please make sure your configuration file is located at /etc/td-agent/td-agent.conf
```sh
$ sudo /etc/init.d/td-agent start
$ sudo /etc/init.d/td-agent stop
$ sudo /etc/init.d/td-agent restart
$ sudo /etc/init.d/td-agent status
```

#### Post Sample Logs
By default, /etc/td-agent/td-agent.conf is configured to take logs from HTTP and route them to stdout (/var/log/td-agent/td-agent.log).
```sh
$ curl -X POST -d 'json={"json":"message"}' http://localhost:8888/debug.test
```

## Elastricsearch
8G 이하의 서버에서는 정상작동 안할 수 있음 [출처](https://github.com/SDSACT/coe-guide/blob/master/Log/EFK_noDocker.md)

#### yum package
```
$ sudo rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch
```
repository정보 추가 /etc/yum.repos.d/elasticsearch.repo  
```
[elasticsearch-6.x]
name=Elasticsearch repository for 6.x packages
baseurl=https://artifacts.elastic.co/packages/6.x/yum
gpgcheck=1
gpgkey=https://artifacts.elastic.co/GPG-KEY-elasticsearch
enabled=1
autorefresh=1
type=rpm-md
```
```
$ sudo yum update
$ sudo yum install elasticsearch
```
[출처](https://www.elastic.co/guide/en/elasticsearch/reference/current/rpm.html)
#### Configuring elasticsearch
설정파일 경로
/etc/elasticsearch/elasticsearch.yml
/etc/sysconfig/elasticsearch
[confi 항목 상세](https://www.elastic.co/guide/en/elasticsearch/reference/current/rpm.html#rpm-configuring)

#### Running kibana with systemd
서버 시작시 자동 실행 등록
```
sudo /bin/systemctl daemon-reload
sudo /bin/systemctl enable elasticsearch.service
```
서비스 시작
```
sudo systemctl start elasticsearch.service
sudo systemctl stop elasticsearch.service
```

#### tar 실행
it is recommended that you use **the Oracle JDK version 1.8.0_131**
```sh
curl -L -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.3.1.tar.gz
tar -xvf elasticsearch-6.3.1.tar.gz
cd elasticsearch-6.3.1/bin
./elasticsearch
```
[출처](https://www.elastic.co/guide/en/elasticsearch/reference/current/_installation.html)

## Kibana
#### yum package
```
$ sudo rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch
```
repository정보 추가 /etc/yum.repos.d/kibana.repo  
```
[kibana-6.x]
name=Kibana repository for 6.x packages
baseurl=https://artifacts.elastic.co/packages/6.x/yum
gpgcheck=1
gpgkey=https://artifacts.elastic.co/GPG-KEY-elasticsearch
enabled=1
autorefresh=1
type=rpm-md
```
```
$ sudo yum update
$ sudo yum install kibana
```
[출처](https://www.elastic.co/guide/en/kibana/current/rpm.html)
#### Configuring kibana
설정파일 경로 /etc/kibana/kibana.yml
[confi 항목 상세](https://www.elastic.co/guide/en/kibana/6.3/settings.html)

#### Running kibana with systemd
서버 시작시 자동 실행 등록
```
sudo /bin/systemctl daemon-reload
sudo /bin/systemctl enable kibana.service
```
서비스 시작
```
sudo systemctl start kibana.service
sudo systemctl stop kibana.service
```

#### tar 실행
```sh
wget  https://artifacts.elastic.co/downloads/kibana/kibana-6.3.1-linux-x86_64.tar.gz
shasum -a 512 kibana-6.3.1-linux-x86_64.tar.gz
tar -xzf kibana-6.3.1-linux-x86_64.tar.gz
cd kibana-6.3.1-linux-x86_64/
./bin/kibana
```
[출처](https://www.elastic.co/guide/en/kibana/current/install.html)



## CentOS fluentd 설치 

docker run -v /Users/boston/Developer/data:/data -it centos /bin/bash

* td-agent rpm 설치  
yum localinstall /data/td-agent-3.1.1-0.el7.x86_64.rpm

#### 네트워크 안되는 상황 
1. rpm 설치파일을 로컬에 받음  
yum install /data/td-agent-3.1.1-0.el7.x86_64.rpm --downloaddir=/data/rpms/ --downloadonly
2. 해당 파일 모두 설치  
yum install ./rpms/*.rpm
3. td-agent rpm 설치  
yum install /data/td-agent-3.1.1-0.el7.x86_64.rpm

https://www.centos.org/forums/viewtopic.php?t=62995  

* plugin 설치  
/opt/td-agent/embeded/bin/fluent-gem install --force --local /data/fluent-plugin-elasticsearch-2.11.1.gem

#### 설치경로 참고
* 설치 경로 
/opt/td-agent
* 로그 경로
/var/log/td-agent
* config 경로
/etc/td-agent  
/etc/init.d/td-agent status
* plugin   
/opt/td-agent/embedded/lib/ruby/gems/2.4.0/gems

### td-agent/plugin 설치
```bash
# rpm --import https://packages.treasuredata.com/GPG-KEY-td-agent
# vi /etc/yum.repos.d/td.repo

[treasuredata]
name=TreasureData
baseurl=http://packages.treasuredata.com/3/redhat/\$releasever/\$basearch
gpgcheck=1
gpgkey=https://packages.treasuredata.com/GPG-KEY-td-agent

# yum check-update
# yum install td-agent

# /etc/init.d/td-agent start
```


scp -i ~/Downloads/fluentd.pem ~/Developer/data/fluent-plugin-elasticsearch-2.11.1.gem ec2-user@ec2-52-79-117-82.ap-northeast-2.compute.amazonaws.com:~/.

```bash
# /opt/td-agent/embedded/bin/fluent-gem install fluent-plugin-elasticsearch -v 2.7.0
# /opt/td-agent/embedded/bin/fluent-gem install fluent-plugin-grok-parser -v 2.1.6
# /etc/init.d/td-agent restart
```

### 환경설정 
```bash
# vi /etc/td-agent/td-agent.conf

### fluentd aggregator ###
<system>
    @log_level info
</system>

<source>
    @type forward
    bind 0.0.0.0
    port 24224
    @log_level debug
</source>

<match app.**>
    @type copy
    <store>
        @type elasticsearch
        host 52.79.248.125
        port 9200
        logstash_format true
        logstash_prefix dep_api
        logstash_dateformat %Y%m%d
        time_key timestamp
        include_tag_key true
        type_name app_log
        tag_key @log_name
        buffer_type memory
        buffer_chunk_limit 16m
        buffer_queue_limit 128
        flush_interval 30s
        flush_at_shutdown false
        retry_limit 17
        retry_wait 1s
        max_retry_wait 1m
        disable_retry_limit true
        num_threads 4
    </store>
    <store>
        @type stdout
    </store>
</match>
```

#### service 등록
```bash
# /bin/systemctl daemon-reload
# /bin/systemctl enable td-agent.service

# service td-agent.service start
```
log, pid path 필요 시 수정 (/usr/lib/systemd/system/td-agent.service)






