# fakecore

[![License](https://img.shields.io/github/license/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Language](https://img.shields.io/github/languages/top/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.esrrhs/fakecore)](https://central.sonatype.com/artifact/com.github.esrrhs/fakecore)
[![Build Status](https://github.com/esrrhs/fakecore/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/esrrhs/fakecore/actions)

Card/board game server framework with networking, config, storage, and pluggable table management.

**Languages:** [English](README.md) (primary) · [中文](README_CN.md)

---

## Overview

**fakecore** is a Java library for building card and board game servers. It provides TCP and WebSocket networking (Netty preferred), JSON config loading, file helpers, MySQL (HikariCP) and Redis access, codecs, and a pluggable table/room model.

Current release: **1.1.2** (Java **8** bytecode; CI builds with JDK 17).

## Modules

| Area | What you get |
|------|----------------|
| **Network** | TCP (`TcpServer` / `TcpClient`); WebSocket via `NettyWSServer` / `NettyWSClient` (legacy Java-WebSocket types are `@Deprecated`); message codecs; processor pool |
| **Config / Args / File** | JSON config, CLI/JSON args, file helpers |
| **MySQL** | HikariCP pool through `MySQLMgr`, optional `MySQLConn` wrapper |
| **Redis** | Jedis pool with path / password / pool overrides |
| **Table** | Room/table lifecycle on striped shared executors, timers, `GamePlugin` hooks |
| **Codec / Util** | Base64 / RC4 / encrypt (commons-codec), optional GeoIP, mail helpers |

## Installation

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

## Build, test, and coverage

```bash
./mvnw clean test
./mvnw package
```

- Unit tests: **JUnit Jupiter 5** + Surefire
- Coverage: **JaCoCo** report at `target/site/jacoco/` (soft LINE floor **45%** in `pom.xml`)
- Demo (no Redis/MySQL): [`examples/demo-table`](examples/demo-table)

## CI

GitHub Actions workflow [`.github/workflows/maven.yml`](.github/workflows/maven.yml):

- Triggers: `push` and `pull_request`
- JDK 17 (Temurin) + Maven cache
- Runs `./mvnw -B clean test package`
- Uploads JaCoCo HTML as artifact `jacoco-report`

Dependabot watches Maven and GitHub Actions. Publishing to Maven Central is [`.github/workflows/publish.yml`](.github/workflows/publish.yml).

## Config samples

### redis.json

Default path: `./bin/config/redis.json`. Override with system property `fakecore.redis.config`, env `FAKECORE_REDIS_CONFIG`, ArgsMgr key `redisConfig`, or `RedisMgr.init("/path/to/redis.json")`.

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

// Legacy wrapper: close() returns the connection to Hikari
try (MySQLConn conn = MySQLMgr.getConn()) {
    conn.executeQuery("SELECT 1");
}
```

## GamePlugin sketch

```java
public class MyGame implements GamePlugin {
    public void construct(Table table) {
        table.addState(1, new TableState(table) { /* ... */ });
        table.addState(99, new TableState(table));
        table.setExceptionStateId(99);
    }
    public boolean init(TablePlayerInfo creator, Object param) { return true; }
    // Optional defaults: onPlayerEnter/Leave, onPlayerDisconnect/Reconnect, onDestroy
}
```

## Network notes

- Prefer **`NettyWSServer` / `NettyWSClient`** (+ `NettyWSHandler`). Lifecycle: `ini` → send → `stop` / `close`, similar to TCP.
- `WSServer` / `WSClient` (Java-WebSocket) still compile but are **`@Deprecated`**.

## GeoIP

`GeoLite2-Country.mmdb` is **not** in the default jar. Copy it from [`optional-resources/`](optional-resources/) onto the classpath to enable lookups. `GeoIPUtil.getCountryCode` returns `null` when the DB is missing.

## Migration notes

### 1.0.18 → 1.1.0

- **`Table` no longer extends `Thread`** — loops run on shared striped executors (`TableScheduler`). `TableMgr` entry points unchanged.
- **MySQLMgr** → HikariCP; prefer `getConnection()`. `getFree()` deprecated.
- **RedisMgr** — path override, password, timeout, pool sizing (default path kept).
- Jedis 4.4.x; mysql-connector-j 8.x; Base64 via commons-codec; Netty WebSocket added; GeoIP DB optional.

### 1.1.0 → 1.1.1

- **fastjson → fastjson2** (`com.alibaba.fastjson2`). Callers must switch imports (`com.alibaba.fastjson.*` → `com.alibaba.fastjson2.*`), including `ArgsMgr.getJsonObject()`.
- Dependency refresh on the Java 8 line (Netty 4.1.x, Jedis 4.4.x, logback 1.3.x, Commons, JUnit 5.14.x, plugins).
- Left alone for Java 8 / API safety: HikariCP 4.0.3, Java-WebSocket 1.5.x, javax.mail, Jedis 5+, logback 1.4+, geoip2 3+, Netty 4.2.

### 1.1.1 → 1.1.2

- Broader pure unit tests; JaCoCo line coverage raised; soft LINE gate **45%**.

## Publishing

When the version in `pom.xml` on `master` changes (or on `workflow_dispatch`), [publish.yml](.github/workflows/publish.yml) builds, signs, and publishes via the Sonatype Portal. Secrets: `GPG_PRIVATE_KEY`, `CENTRAL_USERNAME`, `CENTRAL_TOKEN`.

## License

MIT License. See [LICENSE](LICENSE).
