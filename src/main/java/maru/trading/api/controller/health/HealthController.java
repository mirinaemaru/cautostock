package maru.trading.api.controller.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
