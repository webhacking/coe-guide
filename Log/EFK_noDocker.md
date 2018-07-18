# Docker 없이 EFK 설치하기

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

#### Installing Fluentd
- Redhat / CentOS
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
- Launch Daemon
  - systemd
  If you want to customize systemd behaviour, put your td-agent.service into /etc/systemd/system
  ```sh
  $ sudo systemctl start td-agent.service
  $ sudo systemctl status td-agent.service
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
it is recommended that you use **the Oracle JDK version 1.8.0_131**
```sh
curl -L -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.3.1.tar.gz
tar -xvf elasticsearch-6.3.1.tar.gz
cd elasticsearch-6.3.1/bin
./elasticsearch
```
[출처](https://www.elastic.co/guide/en/elasticsearch/reference/current/_installation.html)

## Kibana
#### Download and install Linux 64-bit package
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
# yum install td-agent-3.1.1

# /etc/init.d/td-agent start
```

```bash
# /opt/td-agent/embedded/bin/fluent-gem install fluent-plugin-elasticsearch -v 2.7.0
# /opt/td-agent/embedded/bin/fluent-gem install fluent-plugin-grok-parser -v 2.1.6
# /etc/init.d/td-agent restart
```

### 환경설정 
```bash
# vi /etc/td-agent/td-agent.conf


### elasticsearch 설치






