# redis-monitor
> 基于软件杯赛题缓存高可用产生的代码，用于解决redis节点状态监控，以及故障恢复、主从切换（目前仅针对赛题考虑，硬编码为2个redis节点，后续会修改为适应任意数量节点）

### 赛题描述

现有两台物理机器，分别部署若干个redis节点(测试程序要求提供两个节点)，测试流程包括(测试脚本为test-script/mark.pyc):
1. 插入数据
2. A机器掉电
3. B机器提供服务
4. A机器1分钟内自动恢复服务
5. B机器掉电
6. A机器提供服务
7. B机器1分钟内自动恢复服务
8. 测试结束
(过程中应保证服务正常即插入、查询等操作无故障且数据不丢失)

### 基本架构及环境

基于两台机器(两台centos6.5)，采用主从架构，设置一主一从两个redis节点分别部署于A、B机器，初始设置A机器节点为master，B机器节点为slave

### 项目说明

目前仅关注`org.nuaa.tomax.redismonitor.RedisMonitor`类

基于该类提供服务，启动项目后会监控A、B两个redis节点的状态，若发现其中一个节点断开，会对其进行监测直至其恢复，恢复后会对其执行slaveof 存活节点的命令完成主从切换(使用基于redis的分布式锁保证多个monitor下仅有一个monitor会对其进行主从切换操作，其余monitor会等待该操作完成)

### 项目部署

1. 执行`mvn clean`

2. 执行`mvn package`

3. 会在target下生成可执行的`redismonitor-1.0-SNAPSHOT.jar`

4. 将其部署到redis节点所在的物理机中(需要有java环境，因为只有2台物理机，所以必须在每一台上都部署至少1个monitor)，这里我放的位置为`/usr/local/contest/monitor.jar`

5. 编写启动脚本`monitor-start.sh`(注意修改路径，需修改该文件权限chmod a+x monitor-start.sh)

   ``` sh
   #!/bin/sh
   
   JAVA_HOME=/usr/local/java/jdk1.8.0_171
   JRE_HOME=/usr/local/java/jdk1.8.0_171/jre
   CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar:$JRE_HOME/lib
   PATH=$JAVA_HOME/bin:$PATH
   
   nohup java -jar ip1 port1 ip2 port2 /usr/local/contest/monitor.jar >monitor.log &
   ```
   (若redis节点都使用了默认端口，则这里的port1、port2可省略)

6. 编写启动服务，在/etc/init.d下新建`redismonitor`并编辑，也需修改文件权限为chmod a+x redismonitor

   ``` shell
   #!/bin/bash
   #chkconfig: 2345 99 30
   #processname: redismonitor
   MONITOR_PATH=/usr/local/contest/monitor-start.sh
   case $1 in
           start) $MONITOR_PATH;;
   esac
   ```

7.  使用service redismonitor start命令测试，启动成功后执行chkconfig redismonitor on 设置为开机启动服务

