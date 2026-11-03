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
    <version>1.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github.esrrhs:fakecore:1.1.0'
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
* **fastjson 1.2.x** 暂未迁移到 fastjson2（避免破坏 `Core`/`ConfigMgr`），请注意安全风险并减少新增用法。
* **javax.mail** 保留以兼容编译；后续大版本可考虑 jakarta.mail。
* GeoIP 数据库可选；新增 Netty WebSocket，旧 WS 标记废弃。

## 发布说明

[Publish to Maven Central](.github/workflows/publish.yml) 会监听 `master` 上的 `pom.xml`。当项目版本号发生变化（或手动触发 `workflow_dispatch`）时，会构建、签名并经由 Sonatype Portal 发布到 Maven Central。仓库需配置密钥：`GPG_PRIVATE_KEY`、`CENTRAL_USERNAME`、`CENTRAL_TOKEN`。

## 许可证

MIT License，详见 [LICENSE](LICENSE)。
