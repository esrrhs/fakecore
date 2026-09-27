# fakecore

[![License](https://img.shields.io/github/license/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Language](https://img.shields.io/github/languages/top/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.esrrhs/fakecore)](https://central.sonatype.com/artifact/com.github.esrrhs/fakecore)
[![Build Status](https://github.com/esrrhs/fakecore/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/esrrhs/fakecore/actions)

Card/board game server framework with networking, config, storage, and pluggable table management.

[English](README.md) | [Chinese](README_CN.md)

---

## Overview

**fakecore** is a Java library for building card and board game servers. It provides TCP and WebSocket networking (Netty / Java-WebSocket), configuration loading, file helpers, MySQL and Redis access, codecs, and a pluggable table/room model for game logic.

## Modules

* **Network** — TCP and WebSocket client/server, message codecs, processor pool
* **Config** — JSON-based configuration loading
* **File** — file helpers
* **MySQL** — connection and manager utilities
* **Redis** — Jedis pool manager
* **Table** — pluggable room/table lifecycle and timers
* **Codec / Util** — Base64/RC4/encrypt helpers, GeoIP, mail utilities

## Installation

### Maven

```xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>fakecore</artifactId>
    <version>1.0.18</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github.esrrhs:fakecore:1.0.18'
```

## Building from Source

Build and run tests with the included Maven Wrapper:

```bash
./mvnw clean test
./mvnw package
```

## Publishing

The [Publish to Maven Central](.github/workflows/publish.yml) workflow watches `pom.xml` on `master`. When the project version changes (or on manual `workflow_dispatch`), it builds, signs, and publishes to Maven Central via the Sonatype Portal. Repository secrets required: `GPG_PRIVATE_KEY`, `CENTRAL_USERNAME`, `CENTRAL_TOKEN`.

## License

MIT License. See [LICENSE](LICENSE).
