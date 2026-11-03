# fakecore

[![License](https://img.shields.io/github/license/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Language](https://img.shields.io/github/languages/top/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.esrrhs/fakecore)](https://central.sonatype.com/artifact/com.github.esrrhs/fakecore)
[![Build Status](https://github.com/esrrhs/fakecore/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/esrrhs/fakecore/actions)

棋牌游戏服务器框架，提供网络、配置、存储与插件化牌桌管理。

[English](README.md) | [中文说明](README_CN.md)

---

## 简介

**fakecore** 是用于搭建棋牌类游戏服务器的 Java 库。内置 TCP / WebSocket 网络（推荐 Netty）、配置加载、文件工具、MySQL（HikariCP）与 Redis 访问、编解码，以及可插件化的牌桌/房间模型。

## 模块

* **网络** — TCP（`TcpServer` / `TcpClient`）与 WebSocket（`NettyWSServer` / `NettyWSClient`；旧 Java-WebSocket 类已标记废弃）、消息编解码、处理器池
* **配置** — 基于 JSON 的配置加载
* **文件** — 文件相关工具
* **MySQL** — 通过 `MySQLMgr` 使用 HikariCP，可选 `MySQLConn` 包装
* **Redis** — Jedis 连接池，支持配置路径 / 密码 / 池参数
* **牌桌** — 可插件化房间/牌桌生命周期、条带化共享线程池、定时器、`GamePlugin` 钩子
* **编解码 / 工具** — Base64/RC4/加密（commons-codec）、可选 GeoIP、邮件工具

## 引入依赖

### Maven

```xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>fakecore</artifactId>
    <version>1.1.2</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github.esrrhs:fakecore:1.1.2'
```

## 源码构建

需要 JDK 8+（CI 使用 17；字节码目标仍为 **1.8**）。

```bash
./mvnw clean test
./mvnw package
```

测试阶段会生成 JaCoCo 报告到 `target/site/jacoco/`。

## 配置示例

### redis.json

默认路径：`./bin/config/redis.json`。可通过系统属性 `fakecore.redis.config`、环境变量 `FAKECORE_REDIS_CONFIG`、ArgsMgr 的 `redisConfig`，或 `RedisMgr.init(path)` 覆盖。

```json
{
  "ip": "127.0.0.1",
  "port": 6379,
  "password": "",
  "timeout": 2000,
  "maxTotal": 16,
  "maxIdle": 8,
  "minIdle": 0,
  "database": 0
}
```

### MySQL

```java
MySQLInfo info = new MySQLInfo();
info.setUrl("jdbc:mysql://127.0.0.1:3306/game?useSSL=false&serverTimezone=UTC");
info.setUser("root");
info.setPasswd("secret");
MySQLMgr.init(10, info);

try (Connection c = MySQLMgr.getConnection()) {
    // ...
}
```

## GamePlugin 示意

```java
public class MyGame implements GamePlugin {
    public void construct(Table table) {
        table.addState(1, new TableState(table) { /* ... */ });
        table.addState(99, new TableState(table));
        table.setExceptionStateId(99);
    }
    public boolean init(TablePlayerInfo creator, Object param) { return true; }
    // 可选默认方法：onPlayerEnter/Leave、onPlayerDisconnect/Reconnect、onDestroy
}
```

可运行示例见 [`examples/demo-table`](examples/demo-table)（无需 Redis/MySQL）。

## 网络说明

* 新代码请优先使用 **`NettyWSServer` / `NettyWSClient`**。生命周期与 TCP 类似：`ini` → 发送 → `stop`/`close`。
* 基于 Java-WebSocket 的 `WSServer` / `WSClient` 仍可用，但已 **`@Deprecated`**。

## GeoIP

默认 jar **不再打包** `GeoLite2-Country.mmdb`。可从 [`optional-resources/`](optional-resources/) 复制到应用 classpath。数据库缺失时 `GeoIPUtil.getCountryCode` 返回 `null`。

## 迁移说明（1.0.18 → 1.1.0）

* **Table** 不再继承 `Thread`，消息循环跑在共享条带化线程池（`TableScheduler`）。`TableMgr` 对外入口不变。
* **MySQLMgr** 改为 HikariCP；推荐 `getConnection()`。`getFree()` 已废弃。
* **RedisMgr** 支持路径覆盖、密码、超时与池参数；默认路径兼容。
* Jedis 升级到 4.4.x；mysql-connector-j 8.3.x。
* 自定义 Base64 改为委托 commons-codec，包级 API 保留。
* **fastjson2 2.0.x** 已替换 fastjson 1.2.x。请将 `com.alibaba.fastjson.*` 改为 `com.alibaba.fastjson2.*`（含 `ArgsMgr.getJsonObject()` 返回类型）。
* **javax.mail** 保留以兼容编译；后续大版本可考虑 jakarta.mail。
* GeoIP 数据库可选；新增 Netty WebSocket，旧 WS 标记废弃。

## 迁移说明（1.1.0 → 1.1.1）

* 在保持 **Java 8** 字节码（`maven.compiler.source/target` 1.8）前提下刷新依赖。
* **fastjson → fastjson2**（`com.alibaba.fastjson2:fastjson2:2.0.66`）；调用方需改 import。
* 升级 Netty 4.1.x、Jedis 4.4.x 补丁、logback 1.3.x、mysql-connector-j 8.4.x、geoip2 2.17.x、Apache Commons、JUnit 5.14.x 及构建插件。
* 为兼容 Java 8 / API 稳定性刻意未改：HikariCP 4.0.3、Java-WebSocket 1.5.7（不升 1.6）、javax.mail 1.6.2（不改 jakarta.mail）、Jedis 5+、logback 1.4+、geoip2 3+、JUnit 6、Netty 4.2。

## 迁移说明（1.1.1 → 1.1.2）

* 扩充纯单元测试（避免真实 Redis/MySQL/网络绑定），提升 JaCoCo 覆盖率。
* 在 `pom.xml` 中设置软性 JaCoCo LINE 覆盖率下限 45%。

## 发布说明

[Publish to Maven Central](.github/workflows/publish.yml) 会监听 `master` 上的 `pom.xml`。当项目版本号发生变化（或手动触发 `workflow_dispatch`）时，会构建、签名并经由 Sonatype Portal 发布到 Maven Central。仓库需配置密钥：`GPG_PRIVATE_KEY`、`CENTRAL_USERNAME`、`CENTRAL_TOKEN`。

## 许可证

MIT License，详见 [LICENSE](LICENSE)。
