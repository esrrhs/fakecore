package com.github.esrrhs.fakecore.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Isolated temporary MySQL and Redis for integration tests.
 * <p>
 * Prefers Testcontainers when Docker is available. Otherwise starts official
 * {@code mysqld} / {@code redis-server} processes bound only to 127.0.0.1 on
 * freshly allocated ports, with unique data directories. Never connects to an
 * already-running service (including the conventional 3306 / 6379 ports).
 * <p>
 * Existing official binaries may be reused as executables only; their data
 * directories and sockets are never used.
 */
public final class TemporaryDataServices implements AutoCloseable
{
	private static final Logger log = LoggerFactory.getLogger(TemporaryDataServices.class);

	public static final String MYSQL_IMAGE = "mysql:8.0.43";
	public static final String REDIS_IMAGE = "redis:7.4-alpine";
	public static final String DATABASE = "fakecore_it";

	private static final Object LOCK = new Object();
	private static TemporaryDataServices instance;
	private static int refs;
	private static boolean hookRegistered;

	private final String mode;
	private final String mysqlJdbcUrl;
	private final String mysqlUser;
	private final String mysqlPassword;
	private final String redisHost;
	private final int redisPort;

	private MySQLContainer<?> mysqlContainer;
	private GenericContainer<?> redisContainer;
	private Process mysqlProcess;
	private Process redisProcess;
	private Path workDir;
	private boolean closed;

	private TemporaryDataServices(String mode, String mysqlJdbcUrl, String mysqlUser, String mysqlPassword,
			String redisHost, int redisPort)
	{
		this.mode = mode;
		this.mysqlJdbcUrl = mysqlJdbcUrl;
		this.mysqlUser = mysqlUser;
		this.mysqlPassword = mysqlPassword;
		this.redisHost = redisHost;
		this.redisPort = redisPort;
	}

	public static TemporaryDataServices acquire()
	{
		synchronized (LOCK)
		{
			if (instance == null)
			{
				instance = startNew();
				if (!hookRegistered)
				{
					Runtime.getRuntime().addShutdownHook(new Thread(() -> {
						synchronized (LOCK)
						{
							if (instance != null)
							{
								instance.closeQuietly();
								instance = null;
								refs = 0;
							}
						}
					}, "fakecore-it-cleanup"));
					hookRegistered = true;
				}
			}
			refs++;
			return instance;
		}
	}

	public static void release()
	{
		synchronized (LOCK)
		{
			if (instance == null)
			{
				return;
			}
			refs--;
			if (refs <= 0)
			{
				instance.closeQuietly();
				instance = null;
				refs = 0;
			}
		}
	}

	public String mode()
	{
		return mode;
	}

	public String mysqlJdbcUrl()
	{
		return mysqlJdbcUrl;
	}

	public String mysqlUser()
	{
		return mysqlUser;
	}

	public String mysqlPassword()
	{
		return mysqlPassword;
	}

	public String redisHost()
	{
		return redisHost;
	}

	public int redisPort()
	{
		return redisPort;
	}

	private static TemporaryDataServices startNew()
	{
		boolean forceProcess = booleanFlag("fakecore.it.forceProcess", "FAKECORE_IT_FORCE_PROCESS");
		boolean forceDocker = booleanFlag("fakecore.it.forceDocker", "FAKECORE_IT_FORCE_DOCKER");
		if (forceDocker && forceProcess)
		{
			throw new IllegalStateException("fakecore.it.forceDocker and fakecore.it.forceProcess cannot both be set");
		}
		if (!forceProcess && dockerAvailable())
		{
			log.info("Starting TemporaryDataServices with Testcontainers");
			return startWithTestcontainers();
		}
		if (forceDocker)
		{
			throw new IllegalStateException(
					"Docker was required for integration tests (fakecore.it.forceDocker) but is not available. "
							+ "Install Docker or unset the flag to use isolated mysqld/redis-server processes.");
		}
		log.info("Docker not used; starting TemporaryDataServices with isolated processes");
		return startWithProcesses();
	}

	static boolean dockerAvailable()
	{
		try
		{
			return DockerClientFactory.instance().isDockerAvailable();
		}
		catch (Throwable t)
		{
			log.info("Docker is not available: {}", t.toString());
			return false;
		}
	}

	private static boolean booleanFlag(String property, String env)
	{
		String v = firstNonBlank(System.getProperty(property), System.getenv(env));
		return v != null && ("true".equalsIgnoreCase(v) || "1".equals(v));
	}

	private static TemporaryDataServices startWithTestcontainers()
	{
		TemporaryDataServices services = new TemporaryDataServices("testcontainers", null, null, null, null, 0);
		try
		{
			MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE))
					.withDatabaseName(DATABASE).withUsername("fakecore").withPassword("fakecore-it")
					.withReuse(false);
			GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
					.withExposedPorts(6379).withCommand("redis-server", "--appendonly", "no", "--save", "")
					.waitingFor(Wait.forListeningPort()).withStartupTimeout(Duration.ofMinutes(2));

			mysql.start();
			redis.start();
			services.mysqlContainer = mysql;
			services.redisContainer = redis;

			return new TemporaryDataServices("testcontainers", mysql.getJdbcUrl(), mysql.getUsername(),
					mysql.getPassword(), redis.getHost(), redis.getMappedPort(6379)).withContainers(mysql, redis);
		}
		catch (RuntimeException e)
		{
			services.closeQuietly();
			throw new IllegalStateException("Failed to start MySQL/Redis with Testcontainers: " + e.getMessage(), e);
		}
	}

	private TemporaryDataServices withContainers(MySQLContainer<?> mysql, GenericContainer<?> redis)
	{
		this.mysqlContainer = mysql;
		this.redisContainer = redis;
		return this;
	}

	private static TemporaryDataServices startWithProcesses()
	{
		Path mysqld = findMysqld();
		Path redisServer = findRedisServer();
		if (mysqld == null || redisServer == null)
		{
			throw new IllegalStateException(missingBinaryMessage(mysqld, redisServer));
		}

		Path workDir = Paths.get(System.getProperty("java.io.tmpdir"), "fakecore-it-" + UUID.randomUUID());
		try
		{
			Files.createDirectories(workDir);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Unable to create isolation dir " + workDir, e);
		}

		TemporaryDataServices services = new TemporaryDataServices("process", null, null, null, null, 0);
		services.workDir = workDir;
		try
		{
			int redisPort = TestSockets.freePort();
			int mysqlPort = TestSockets.freePort();
			services.redisProcess = startRedis(redisServer, workDir.resolve("redis"), redisPort);
			waitForPort("127.0.0.1", redisPort, 20_000);

			Path mysqlData = workDir.resolve("mysql");
			Files.createDirectories(mysqlData);
			Path basedir = mysqld.getParent().getParent();
			initializeMysql(mysqld, basedir, mysqlData);
			services.mysqlProcess = startMysqld(mysqld, basedir, mysqlData, mysqlPort);
			waitForPort("127.0.0.1", mysqlPort, 60_000);
			String jdbcBase = "jdbc:mysql://127.0.0.1:" + mysqlPort
					+ "/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000";
			createDatabase(String.format(jdbcBase, ""), "root", "");
			String jdbcUrl = String.format(jdbcBase, DATABASE);

			return new TemporaryDataServices("process", jdbcUrl, "root", "", "127.0.0.1", redisPort)
					.withProcesses(services.mysqlProcess, services.redisProcess, workDir);
		}
		catch (RuntimeException e)
		{
			services.closeQuietly();
			throw e;
		}
		catch (Exception e)
		{
			services.closeQuietly();
			throw new IllegalStateException("Failed to start isolated mysqld/redis-server: " + e.getMessage(), e);
		}
	}

	private TemporaryDataServices withProcesses(Process mysql, Process redis, Path workDir)
	{
		this.mysqlProcess = mysql;
		this.redisProcess = redis;
		this.workDir = workDir;
		return this;
	}

	private static String missingBinaryMessage(Path mysqld, Path redisServer)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Integration tests need Docker (Testcontainers) or isolated official binaries.\n");
		if (mysqld == null)
		{
			sb.append("mysqld was not found. Set FAKECORE_IT_MYSQL_BASEDIR or property fakecore.it.mysql.basedir ")
					.append("to an official MySQL generic Linux basedir (bin/mysqld).\n");
		}
		if (redisServer == null)
		{
			sb.append("redis-server was not found. Set FAKECORE_IT_REDIS_SERVER or property ")
					.append("fakecore.it.redis.server to a redis-server executable.\n");
		}
		sb.append("This is a failure, not a skip: provide Docker or the binaries and re-run ./mvnw verify.");
		return sb.toString();
	}

	private static Path findMysqld()
	{
		String configured = firstNonBlank(System.getProperty("fakecore.it.mysql.basedir"),
				System.getenv("FAKECORE_IT_MYSQL_BASEDIR"));
		if (configured != null)
		{
			Path mysqld = Paths.get(configured).resolve("bin").resolve("mysqld");
			if (Files.isRegularFile(mysqld))
			{
				return mysqld.toAbsolutePath();
			}
			throw new IllegalStateException("fakecore.it.mysql.basedir does not contain bin/mysqld: " + configured);
		}
		List<Path> roots = new ArrayList<>();
		roots.add(Paths.get("target", "it-deps"));
		roots.add(Paths.get(System.getProperty("user.home"), ".cache", "fakecore-it"));
		roots.add(Paths.get("/tmp", "fakecore-it-deps"));
		for (Path root : roots)
		{
			Path found = findNamed(root, 4, "mysqld");
			if (found != null && found.getParent() != null && "bin".equals(found.getParent().getFileName().toString()))
			{
				return found;
			}
		}
		return findUnderTmp(new String[] { "mysql-*-linux-glibc*/bin/mysqld", "mysql-*/bin/mysqld" });
	}

	private static Path findRedisServer()
	{
		String configured = firstNonBlank(System.getProperty("fakecore.it.redis.server"),
				System.getenv("FAKECORE_IT_REDIS_SERVER"));
		if (configured != null)
		{
			Path p = Paths.get(configured);
			if (Files.isRegularFile(p) && Files.isExecutable(p))
			{
				return p.toAbsolutePath();
			}
			throw new IllegalStateException("fakecore.it.redis.server is not an executable: " + configured);
		}
		String onPath = which("redis-server");
		if (onPath != null)
		{
			return Paths.get(onPath);
		}
		List<Path> roots = new ArrayList<>();
		roots.add(Paths.get("target", "it-deps"));
		roots.add(Paths.get(System.getProperty("user.home"), ".cache", "fakecore-it"));
		roots.add(Paths.get("/tmp", "fakecore-it-deps"));
		for (Path root : roots)
		{
			Path found = findNamed(root, 4, "redis-server");
			if (found != null)
			{
				return found;
			}
		}
		return findUnderTmp(new String[] { "redis/src/redis-server", "redis-server" });
	}

	private static Path findNamed(Path root, int maxDepth, String fileName)
	{
		if (root == null || !Files.isDirectory(root))
		{
			return null;
		}
		try (Stream<Path> stream = Files.walk(root, maxDepth))
		{
			return stream.filter(p -> fileName.equals(p.getFileName().toString()) && Files.isRegularFile(p)
					&& Files.isExecutable(p)).findFirst().orElse(null);
		}
		catch (IOException e)
		{
			return null;
		}
	}

	/**
	 * Shallow search under /tmp/<child>/... only. Reuses already-extracted official
	 * binaries as executables; never opens their data directories or default ports.
	 */
	private static Path findUnderTmp(String[] relativeGlobs)
	{
		Path tmp = Paths.get("/tmp");
		if (!Files.isDirectory(tmp))
		{
			return null;
		}
		try (DirectoryStream<Path> dirs = Files.newDirectoryStream(tmp))
		{
			for (Path dir : dirs)
			{
				if (!Files.isDirectory(dir))
				{
					continue;
				}
				for (String rel : relativeGlobs)
				{
					Path found = matchRelative(dir, rel);
					if (found != null)
					{
						return found;
					}
				}
			}
		}
		catch (IOException ignored)
		{
		}
		return null;
	}

	private static Path matchRelative(Path root, String relativeGlob)
	{
		String[] parts = relativeGlob.split("/");
		return matchParts(root, parts, 0);
	}

	private static Path matchParts(Path current, String[] parts, int index)
	{
		if (index >= parts.length)
		{
			return Files.isRegularFile(current) ? current.toAbsolutePath() : null;
		}
		String part = parts[index];
		boolean glob = part.contains("*");
		if (!glob)
		{
			return matchParts(current.resolve(part), parts, index + 1);
		}
		if (!Files.isDirectory(current))
		{
			return null;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(current, part))
		{
			for (Path child : stream)
			{
				Path found = matchParts(child, parts, index + 1);
				if (found != null)
				{
					return found;
				}
			}
		}
		catch (IOException ignored)
		{
		}
		return null;
	}

	private static String which(String exe)
	{
		String path = System.getenv("PATH");
		if (path == null)
		{
			return null;
		}
		for (String dir : path.split(":"))
		{
			if (dir.isEmpty())
			{
				continue;
			}
			Path candidate = Paths.get(dir, exe);
			if (Files.isExecutable(candidate) && Files.isRegularFile(candidate))
			{
				return candidate.toAbsolutePath().toString();
			}
		}
		return null;
	}

	private static Process startRedis(Path redisServer, Path dir, int port) throws IOException
	{
		Files.createDirectories(dir);
		ProcessBuilder pb = new ProcessBuilder(redisServer.toString(), "--bind", "127.0.0.1", "--port",
				Integer.toString(port), "--dir", dir.toString(), "--dbfilename", "dump.rdb", "--save", "",
				"--appendonly", "no", "--protected-mode", "no", "--daemonize", "no");
		pb.redirectErrorStream(true);
		pb.redirectOutput(dir.resolve("redis.log").toFile());
		Process p = pb.start();
		if (!p.isAlive())
		{
			throw new IllegalStateException("redis-server exited immediately, see " + dir.resolve("redis.log"));
		}
		return p;
	}

	private static void initializeMysql(Path mysqld, Path basedir, Path datadir) throws IOException, InterruptedException
	{
		ProcessBuilder pb = new ProcessBuilder(mysqld.toString(), "--initialize-insecure", "--basedir=" + basedir,
				"--datadir=" + datadir, "--log-error=" + datadir.resolve("init.err"));
		applyLibaio(pb, basedir);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0)
		{
			String err = readQuietly(datadir.resolve("init.err"));
			p.destroyForcibly();
			throw new IllegalStateException("mysqld --initialize-insecure failed: " + err);
		}
	}

	private static Process startMysqld(Path mysqld, Path basedir, Path datadir, int port) throws IOException
	{
		Path socket = datadir.resolve("mysql.sock");
		ProcessBuilder pb = new ProcessBuilder(mysqld.toString(), "--basedir=" + basedir, "--datadir=" + datadir,
				"--bind-address=127.0.0.1", "--port=" + port, "--socket=" + socket, "--pid-file=" + datadir.resolve("mysql.pid"),
				"--log-error=" + datadir.resolve("mysqld.err"), "--mysqlx=0");
		applyLibaio(pb, basedir);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		if (!p.isAlive())
		{
			throw new IllegalStateException("mysqld exited immediately: " + readQuietly(datadir.resolve("mysqld.err")));
		}
		return p;
	}

	private static void applyLibaio(ProcessBuilder pb, Path basedir)
	{
		Path libaio = findLibaio(basedir);
		if (libaio == null)
		{
			return;
		}
		String current = pb.environment().getOrDefault("LD_LIBRARY_PATH", System.getenv().getOrDefault("LD_LIBRARY_PATH", ""));
		String add = libaio.toString();
		pb.environment().put("LD_LIBRARY_PATH", current == null || current.isEmpty() ? add : add + ":" + current);
	}

	private static Path findLibaio(Path basedir)
	{
		String configured = firstNonBlank(System.getProperty("fakecore.it.libaio.dir"),
				System.getenv("FAKECORE_IT_LIBAIO_DIR"));
		if (configured != null)
		{
			return Paths.get(configured);
		}
		List<Path> candidates = new ArrayList<>();
		if (basedir != null)
		{
			candidates.add(basedir.resolve("lib"));
			candidates.add(basedir.resolve("lib").resolve("private"));
		}
		candidates.add(Paths.get("/tmp", "fakecore-it-deps", "libaio"));
		Path found = findUnderTmp(new String[] { "libaio-prefix/lib/libaio.so.1", "libaio/src/libaio.so.1", "libaio.so.1" });
		if (found != null)
		{
			return found.getParent();
		}
		for (Path c : candidates)
		{
			if (Files.isRegularFile(c.resolve("libaio.so.1")))
			{
				return c;
			}
		}
		return null;
	}

	private static void createDatabase(String jdbcUrl, String user, String password) throws Exception
	{
		long deadline = System.currentTimeMillis() + 30_000;
		Exception last = null;
		while (System.currentTimeMillis() < deadline)
		{
			try (Connection c = DriverManager.getConnection(jdbcUrl, user, password); Statement s = c.createStatement())
			{
				s.execute("CREATE DATABASE IF NOT EXISTS " + DATABASE);
				return;
			}
			catch (Exception e)
			{
				last = e;
				Thread.sleep(200);
			}
		}
		throw new IllegalStateException("Unable to create database " + DATABASE, last);
	}

	private static void waitForPort(String host, int port, long timeoutMs)
	{
		long deadline = System.currentTimeMillis() + timeoutMs;
		Exception last = null;
		while (System.currentTimeMillis() < deadline)
		{
			try (Socket s = new Socket())
			{
				s.connect(new InetSocketAddress(host, port), 200);
				return;
			}
			catch (Exception e)
			{
				last = e;
				try
				{
					Thread.sleep(50);
				}
				catch (InterruptedException ie)
				{
					Thread.currentThread().interrupt();
					throw new IllegalStateException("interrupted waiting for " + host + ":" + port, ie);
				}
			}
		}
		throw new IllegalStateException("Port not ready: " + host + ":" + port, last);
	}

	private static String readQuietly(Path path)
	{
		try
		{
			if (Files.isRegularFile(path))
			{
				return Files.readString(path);
			}
		}
		catch (IOException ignored)
		{
		}
		return "(no log at " + path + ")";
	}

	private static String firstNonBlank(String a, String b)
	{
		if (a != null && !a.trim().isEmpty())
		{
			return a.trim();
		}
		if (b != null && !b.trim().isEmpty())
		{
			return b.trim();
		}
		return null;
	}

	private void closeQuietly()
	{
		try
		{
			close();
		}
		catch (Exception e)
		{
			log.warn("TemporaryDataServices close", e);
		}
	}

	@Override
	public synchronized void close()
	{
		if (closed)
		{
			return;
		}
		closed = true;
		stopProcess(redisProcess);
		stopProcess(mysqlProcess);
		redisProcess = null;
		mysqlProcess = null;
		if (redisContainer != null)
		{
			try
			{
				redisContainer.stop();
			}
			catch (Exception e)
			{
				log.warn("stop redis container", e);
			}
			redisContainer = null;
		}
		if (mysqlContainer != null)
		{
			try
			{
				mysqlContainer.stop();
			}
			catch (Exception e)
			{
				log.warn("stop mysql container", e);
			}
			mysqlContainer = null;
		}
		deleteRecursively(workDir);
		workDir = null;
	}

	private static void stopProcess(Process p)
	{
		if (p == null || !p.isAlive())
		{
			return;
		}
		p.destroy();
		try
		{
			if (!p.waitFor(8, TimeUnit.SECONDS))
			{
				p.destroyForcibly();
				p.waitFor(5, TimeUnit.SECONDS);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			p.destroyForcibly();
		}
	}

	private static void deleteRecursively(Path root)
	{
		if (root == null || !Files.exists(root))
		{
			return;
		}
		try (Stream<Path> stream = Files.walk(root))
		{
			stream.sorted(Comparator.reverseOrder()).forEach(p -> {
				try
				{
					Files.deleteIfExists(p);
				}
				catch (IOException ignored)
				{
				}
			});
		}
		catch (IOException ignored)
		{
		}
	}
}
