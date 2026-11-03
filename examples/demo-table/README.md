# fakecore demo-table

Minimal runnable example: a fake `GamePlugin` that registers WAIT / PLAY / END / exception states and drives transitions with a message + timer. **No Redis or MySQL required.**

## Prerequisites

Install the parent library into the local Maven repo from the repository root:

```bash
./mvnw -B clean install -DskipTests
```

Or run tests then install:

```bash
./mvnw -B clean install
```

## Run

```bash
./mvnw -f examples/demo-table/pom.xml -q exec:java
```

Expected log lines include `[WAIT]`, `[PLAY]`, `[END]`, and `demo finished destroy=true`.

## Config samples (not needed for this demo)

See the root README for `redis.json` / MySQL samples used by real game servers.
