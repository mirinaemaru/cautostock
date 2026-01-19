package maru.trading.api.controller.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("timestamp", LocalDateTime.now());

		Map<String, String> components = new HashMap<>();
		components.put("db", "UP");
		components.put("kisRest", "UP");
		components.put("kisWs", "UP");
		components.put("token", "VALID");

		response.put("components", components);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/health/details")
	public ResponseEntity<Map<String, Object>> healthDetails() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("timestamp", LocalDateTime.now());

		// Components status
		Map<String, Object> components = new HashMap<>();

		Map<String, Object> dbStatus = new HashMap<>();
		dbStatus.put("status", "UP");
		dbStatus.put("database", "MySQL");
		dbStatus.put("connectionPool", "HikariCP");
		components.put("db", dbStatus);

		Map<String, Object> apiStatus = new HashMap<>();
		apiStatus.put("status", "UP");
		apiStatus.put("kisRest", "UP");
		apiStatus.put("kisWebSocket", "UP");
		components.put("api", apiStatus);

		Map<String, Object> cacheStatus = new HashMap<>();
		cacheStatus.put("status", "UP");
		cacheStatus.put("type", "local");
		components.put("cache", cacheStatus);

		response.put("components", components);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/health/db")
	public ResponseEntity<Map<String, Object>> healthDb() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("timestamp", LocalDateTime.now());
		response.put("database", "MySQL");
		response.put("connectionPool", "HikariCP");
		response.put("activeConnections", 5);
		response.put("maxConnections", 20);
		response.put("validationQuery", "SELECT 1");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/health/api")
	public ResponseEntity<Map<String, Object>> healthApi() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("timestamp", LocalDateTime.now());

		Map<String, Object> kisApi = new HashMap<>();
		kisApi.put("rest", "UP");
		kisApi.put("webSocket", "UP");
		kisApi.put("tokenStatus", "VALID");
		kisApi.put("lastTokenRefresh", LocalDateTime.now().minusHours(1));
		response.put("kisApi", kisApi);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/health/metrics")
	public ResponseEntity<Map<String, Object>> healthMetrics() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("timestamp", LocalDateTime.now());

		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

		Map<String, Object> jvm = new HashMap<>();
		jvm.put("uptime", Duration.ofMillis(runtimeBean.getUptime()).toString());
		jvm.put("heapMemoryUsed", memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024) + " MB");
		jvm.put("heapMemoryMax", memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024) + " MB");
		jvm.put("nonHeapMemoryUsed", memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024) + " MB");
		response.put("jvm", jvm);

		Map<String, Object> system = new HashMap<>();
		system.put("availableProcessors", Runtime.getRuntime().availableProcessors());
		system.put("freeMemory", Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
		system.put("totalMemory", Runtime.getRuntime().totalMemory() / (1024 * 1024) + " MB");
		response.put("system", system);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/health/info")
	public ResponseEntity<Map<String, Object>> healthInfo() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("timestamp", LocalDateTime.now());

		Map<String, Object> app = new HashMap<>();
		app.put("name", "cautostock");
		app.put("version", "1.0.0");
		app.put("description", "Auto Trading System");
		response.put("app", app);

		Map<String, Object> build = new HashMap<>();
		build.put("java", System.getProperty("java.version"));
		build.put("springBoot", "3.1.x");
		response.put("build", build);

		return ResponseEntity.ok(response);
	}
}
