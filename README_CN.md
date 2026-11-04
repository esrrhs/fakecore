# fakecore

[![License](https://img.shields.io/github/license/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Language](https://img.shields.io/github/languages/top/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.esrrhs/fakecore)](https://central.sonatype.com/artifact/com.github.esrrhs/fakecore)
[![Build Status](https://github.com/esrrhs/fakecore/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/esrrhs/fakecore/actions)

棋牌游戏服务器框架：网络、配置、存储与插件化牌桌管理。

**语言：** [English](README.md)（主文档） · [中文](README_CN.md)（本文）

---

## 简介

**fakecore** 是用于搭建棋牌类游戏服务器的 Java 库。提供 TCP / WebSocket 网络（推荐 Netty）、JSON 配置、文件工具、MySQL（HikariCP）与 Redis、编解码，以及可插件化的牌桌/房间模型。

当前版本：**1.2.0**（需要 **Java 17**；CI 使用 JDK 17 Temurin）。

完整说明以英文 [README.md](README.md) 为准；本文为中文对照。

## 模块

| 模块 | 内容 |
|------|------|
| **网络** | TCP（`TcpServer` / `TcpClient`）；WebSocket 推荐 `NettyWSServer` / `NettyWSClient`（旧 Java-WebSocket 已 `@Deprecated`）；消息编解码；处理器池 |
| **配置 / 参数 / 文件** | JSON 配置、参数解析、文件工具 |
| **MySQL** | `MySQLMgr` + HikariCP，可选 `MySQLConn` |
| **Redis** | Jedis 连接池，支持路径 / 密码 / 池参数覆盖 |
| **牌桌** | 房间/牌桌生命周期（条带化共享线程池）、定时器、`GamePlugin` 钩子 |
| **编解码 / 工具** | Base64 / RC4 / 加密（commons-codec）、可选 GeoIP、邮件工具 |

## 引入依赖

### Maven

```xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>fakecore</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github.esrrhs:fakecore:1.2.0'
```

## 构建、测试与覆盖率

```bash
./mvnw clean test
./mvnw package
```

- 单元测试：**JUnit Jupiter 6** + Surefire
- 覆盖率：**JaCoCo**，报告在 `target/site/jacoco/`（`pom.xml` 中 LINE 软门槛 **45%**）
- 示例（无需 Redis/MySQL）：[`examples/demo-table`](examples/demo-table)

## CI

工作流 [`.github/workflows/maven.yml`](.github/workflows/maven.yml)：

- 触发：`push`、`pull_request`
- JDK 17（Temurin）+ Maven 缓存
- 执行 `./mvnw -B clean test package`
- 上传 JaCoCo HTML 产物 `jacoco-report`

Dependabot 跟踪 Maven 与 GitHub Actions。发布见 [`.github/workflows/publish.yml`](.github/workflows/publish.yml)。

## 配置示例

### redis.json

默认路径：`./bin/config/redis.json`。可用系统属性 `fakecore.redis.config`、环境变量 `FAKECORE_REDIS_CONFIG`、ArgsMgr 的 `redisConfig`，或 `RedisMgr.init(path)` 覆盖。

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
    // 可选：onPlayerEnter/Leave、onPlayerDisconnect/Reconnect、onDestroy
}
```

## 网络说明

- 新代码请用 **`NettyWSServer` / `NettyWSClient`**。生命周期与 TCP 类似：`ini` → 发送 → `stop` / `close`。
- `WSServer` / `WSClient` 仍可编译，但已 **`@Deprecated`**。

## GeoIP

默认 jar **不打包** `GeoLite2-Country.mmdb`。可从 [`optional-resources/`](optional-resources/) 拷到 classpath。数据库缺失时 `GeoIPUtil.getCountryCode` 返回 `null`。

## 迁移说明

### 1.0.18 → 1.1.0

- **`Table` 不再继承 `Thread`**，消息循环在 `TableScheduler` 条带化线程池上；`TableMgr` 对外入口不变。
- **MySQLMgr** 改为 HikariCP，推荐 `getConnection()`。
- **RedisMgr** 支持路径 / 密码 / 超时 / 池参数；默认路径兼容。
- Jedis 4.4.x、mysql-connector-j 8.x、commons-codec Base64、Netty WebSocket、GeoIP 可选。

### 1.1.0 → 1.1.1

- **fastjson → fastjson2**，请将 `com.alibaba.fastjson.*` 改为 `com.alibaba.fastjson2.*`（含 `ArgsMgr.getJsonObject()`）。
- 在 Java 8 线上刷新 Netty / Jedis / logback / Commons / JUnit 等。
- 为兼容刻意未升：HikariCP 5+、Java-WebSocket 1.6、jakarta.mail、Jedis 5+、logback 1.4+、geoip2 3+、Netty 4.2。

### 1.1.1 → 1.1.2

- 扩充纯单元测试，提高 JaCoCo 覆盖率；LINE 软门槛 **45%**。


### 1.1.2 → 1.2.0

- **Java 基线升至 17**（`maven.compiler.source/target=17`），不再支持 Java 8/11。
- **Jedis 4.4.x → 8.0.2** — `RedisMgr` 使用 `HostAndPort` + `DefaultJedisClientConfig` 建池；对外 `init` / `getJedis` / `shutdown` / 配置路径解析不变。自行创建 `JedisPool` 的代码请改用 Jedis 5+ 客户端配置 API。
- **javax.mail → Jakarta Mail**（`org.eclipse.angus:jakarta.mail`）。请将 `javax.mail.*` 改为 `jakarta.mail.*`（`GMailUtil` / `NeteaseMailUtil`）。
- **geoip2 2.x → 5.2.0**（需 Java 17）。可选 MMDB 存在时，`GeoIPUtil` 行为不变。
- **Netty 4.1 → 4.2.20.Final**、**logback 1.3 → 1.6.6**、**mysql-connector-j 8.4 → 26.10.0**；HikariCP 保持 7.1.0。
- 插件刷新：Surefire 3.6.1、resources 3.5.0、central-publishing 0.12.0。Actions 已在 checkout@v7 / setup-java@v6 / upload-artifact@v7；Maven Wrapper 仍为 Apache Maven 3.10.0。
- `fakescript-java` 仍为 **1.0.16**（Maven Central 无更新）。

## 发布

`master` 上 `pom.xml` 版本变更（或手动 `workflow_dispatch`）时，[publish.yml](.github/workflows/publish.yml) 构建、签名并经 Sonatype Portal 发布。需配置：`GPG_PRIVATE_KEY`、`CENTRAL_USERNAME`、`CENTRAL_TOKEN`。

## 许可证

MIT License，详见 [LICENSE](LICENSE)。
