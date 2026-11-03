# fakecore

[![License](https://img.shields.io/github/license/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Language](https://img.shields.io/github/languages/top/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.esrrhs/fakecore)](https://central.sonatype.com/artifact/com.github.esrrhs/fakecore)
[![Build Status](https://github.com/esrrhs/fakecore/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/esrrhs/fakecore/actions)

Card/board game server framework with networking, config, storage, and pluggable table management.

[English](README.md) | [Chinese](README_CN.md)

---

## Overview

**fakecore** is a Java library for building card and board game servers. It provides TCP and WebSocket networking (Netty preferred), configuration loading, file helpers, MySQL (HikariCP) and Redis access, codecs, and a pluggable table/room model for game logic.

## Modules

* **Network** — TCP (`TcpServer` / `TcpClient`) and WebSocket (`NettyWSServer` / `NettyWSClient`; legacy Java-WebSocket classes are deprecated), message codecs, processor pool
* **Config** — JSON-based configuration loading
* **File** — file helpers
* **MySQL** — HikariCP pool via `MySQLMgr`, optional `MySQLConn` wrapper
* **Redis** — Jedis pool manager with configurable path / password / pool settings
* **Table** — pluggable room/table lifecycle, striped shared executors, timers, `GamePlugin` hooks
* **Codec / Util** — Base64/RC4/encrypt helpers (commons-codec), optional GeoIP, mail utilities

## Installation

### Maven

```xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>fakecore</artifactId>
    <version>1.1.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github.esrrhs:fakecore:1.1.1'
```

## Building from Source

Requires JDK 8+ (CI uses 17; bytecode target remains **1.8**).

```bash
./mvnw clean test
./mvnw package
```

JaCoCo coverage reports are written to `target/site/jacoco/` during `test`.

## Config samples

### redis.json

Default path: `./bin/config/redis.json`. Override with:

* system property `fakecore.redis.config`
* env `FAKECORE_REDIS_CONFIG`
* ArgsMgr JSON key `redisConfig`
* or `RedisMgr.init("/path/to/redis.json")`

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

// or legacy wrapper (close returns connection to Hikari pool)
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
    // optional defaults: onPlayerEnter/Leave, onPlayerDisconnect/Reconnect, onDestroy
}
```

See [`examples/demo-table`](examples/demo-table) for a runnable demo (no Redis/MySQL).

## Network notes

* Prefer **`NettyWSServer` / `NettyWSClient`** (+ `NettyWSHandler`) for WebSocket. Lifecycle: `ini` → `send*` → `stop`/`close`, similar to TCP.
* `WSServer` / `WSClient` (Java-WebSocket) remain available but are **`@Deprecated`**.

## GeoIP

`GeoLite2-Country.mmdb` is **not** shipped in the default jar. Copy it from [`optional-resources/`](optional-resources/) onto your classpath to enable lookups. `GeoIPUtil.getCountryCode` returns `null` when the DB is absent.

## Migration notes (1.0.18 → 1.1.0)

* **Table** no longer extends `Thread`. Loops run on shared striped executors (`TableScheduler`). `TableMgr` entry points are unchanged; call `table.start(stateId)` as before.
* **MySQLMgr** uses HikariCP. Prefer `getConnection()`. `getConn()` still works; `close()` returns to the pool. `getFree()` is deprecated/unused.
* **RedisMgr** supports config path override, password, timeout, pool sizing. Default path unchanged.
* **Jedis** upgraded to 4.4.x (API compatible for this wrapper).
* **mysql-connector-j** 8.3.x; driver class `com.mysql.cj.jdbc.Driver`.
* Custom Base64 implementations delegate to **commons-codec**; package APIs kept.
* **fastjson2 2.0.x** replaces fastjson 1.2.x. Update imports from `com.alibaba.fastjson.*` to `com.alibaba.fastjson2.*` (including `ArgsMgr.getJsonObject()` return type).
* **javax.mail** retained for compile compatibility; consider jakarta.mail in a future major.
* GeoIP DB optional (see above).
* New Netty WebSocket types; legacy WS deprecated.

## Migration notes (1.1.0 → 1.1.1)

* Dependency refresh while keeping **Java 8** bytecode (`maven.compiler.source/target` 1.8).
* Migrated **fastjson → fastjson2** (`com.alibaba.fastjson2:fastjson2:2.0.66`); callers must switch imports.
* Bumped Netty 4.1.x, Jedis 4.4.x patch, logback 1.3.x, mysql-connector-j 8.4.x, geoip2 2.17.x, Apache Commons, JUnit 5.14.x, and build plugins.
* Intentionally unchanged for Java 8 / API safety: HikariCP 4.0.3, Java-WebSocket 1.5.7 (not 1.6), javax.mail 1.6.2 (not jakarta.mail), Jedis 5+, logback 1.4+, geoip2 3+, JUnit 6, Netty 4.2.

## Publishing

The [Publish to Maven Central](.github/workflows/publish.yml) workflow watches `pom.xml` on `master`. When the project version changes (or on manual `workflow_dispatch`), it builds, signs, and publishes to Maven Central via the Sonatype Portal. Repository secrets required: `GPG_PRIVATE_KEY`, `CENTRAL_USERNAME`, `CENTRAL_TOKEN`.

## License

MIT License. See [LICENSE](LICENSE).
