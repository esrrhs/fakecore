# fakecore

[![License](https://img.shields.io/github/license/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Language](https://img.shields.io/github/languages/top/esrrhs/fakecore)](https://github.com/esrrhs/fakecore)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.esrrhs/fakecore)](https://central.sonatype.com/artifact/com.github.esrrhs/fakecore)
[![Build Status](https://github.com/esrrhs/fakecore/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/esrrhs/fakecore/actions)

棋牌游戏服务器框架，提供网络、配置、存储与插件化牌桌管理。

[English](README.md) | [中文说明](README_CN.md)

---

## 简介

**fakecore** 是用于搭建棋牌类游戏服务器的 Java 库。内置 TCP / WebSocket 网络（Netty / Java-WebSocket）、配置加载、文件工具、MySQL 与 Redis 访问、编解码，以及可插件化的牌桌/房间模型。

## 模块

* **网络** — TCP、WebSocket 客户端/服务端、消息编解码、处理器池
* **配置** — 基于 JSON 的配置加载
* **文件** — 文件相关工具
* **MySQL** — 连接与管理工具
* **Redis** — Jedis 连接池管理
* **牌桌** — 可插件化的房间/牌桌生命周期与定时器
* **编解码 / 工具** — Base64、RC4、加密、GeoIP、邮件等

## 引入依赖

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

## 源码构建

使用自带的 Maven Wrapper 构建与测试：

```bash
./mvnw clean test
./mvnw package
```

## 发布说明

[Publish to Maven Central](.github/workflows/publish.yml) 会监听 `master` 上的 `pom.xml`。当项目版本号发生变化（或手动触发 `workflow_dispatch`）时，会构建、签名并经由 Sonatype Portal 发布到 Maven Central。仓库需配置密钥：`GPG_PRIVATE_KEY`、`CENTRAL_USERNAME`、`CENTRAL_TOKEN`。

## 许可证

MIT License，详见 [LICENSE](LICENSE)。
