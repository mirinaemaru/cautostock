package maru.trading.api.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Optimization Admin API Controller
 *
 * Endpoints:
 * - GET  /api/v1/admin/optimization           - List all optimizations
 * - POST /api/v1/admin/optimization           - Create optimization
 * - GET  /api/v1/admin/optimization/{id}      - Get optimization details
 * - POST /api/v1/admin/optimization/run       - Run optimization
 * - GET  /api/v1/admin/optimization/methods   - Get available optimization methods
 * - DELETE /api/v1/admin/optimization/{id}    - Delete optimization
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/optimization")
@RequiredArgsConstructor
public class OptimizationAdminController {

    private final List<Map<String, Object>> optimizations = Collections.synchronizedList(new ArrayList<>());
    private int nextId = 1;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listOptimizations(
            @RequestParam(required = false) String strategyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Listing optimizations, strategyId: {}, page: {}, size: {}", strategyId, page, size);

        List<Map<String, Object>> filtered = optimizations;
        if (strategyId != null) {
            filtered = optimizations.stream()
                    .filter(o -> strategyId.equals(o.get("strategyId")))
                    .toList();
        }

        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<Map<String, Object>> pageContent = start < filtered.size()
                ? filtered.subList(start, end)
                : Collections.emptyList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageContent);
        response.put("totalElements", filtered.size());
        response.put("totalPages", (filtered.size() + size - 1) / size);
        response.put("page", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOptimization(@RequestBody Map<String, Object> request) {
        log.info("Creating optimization: {}", request);

        Map<String, Object> optimization = new HashMap<>(request);
        optimization.put("optimizationId", "OPT-" + String.format("%05d", nextId++));
        optimization.put("status", "PENDING");
        optimization.put("createdAt", LocalDateTime.now());
        optimization.put("updatedAt", LocalDateTime.now());

        optimizations.add(optimization);

        return ResponseEntity.ok(optimization);
    }

    @GetMapping("/{optimizationId}")
    public ResponseEntity<Map<String, Object>> getOptimization(@PathVariable String optimizationId) {
        log.info("Getting optimization: {}", optimizationId);

        return optimizations.stream()
                .filter(o -> optimizationId.equals(o.get("optimizationId")))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runOptimization(@RequestBody Map<String, Object> request) {
        log.info("Running optimization: {}", request);

        String strategyId = (String) request.get("strategyId");
        String method = (String) request.getOrDefault("method", "GRID_SEARCH");

        Map<String, Object> result = new HashMap<>();
        result.put("optimizationId", "OPT-" + String.format("%05d", nextId++));
        result.put("strategyId", strategyId);
        result.put("method", method);
        result.put("status", "RUNNING");
        result.put("startedAt", LocalDateTime.now());
        result.put("progress", 0);
        result.put("estimatedCompletion", LocalDateTime.now().plusMinutes(5));

        // Add initial parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stopLoss", Map.of("min", 1, "max", 10, "step", 1));
        parameters.put("takeProfit", Map.of("min", 2, "max", 20, "step", 2));
        result.put("parameters", parameters);

        optimizations.add(result);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/methods")
    public ResponseEntity<Map<String, Object>> getOptimizationMethods() {
        log.info("Getting optimization methods");

        List<Map<String, Object>> methods = new ArrayList<>();

        methods.add(Map.of(
                "id", "GRID_SEARCH",
                "name", "Grid Search",
                "description", "Exhaustive search over specified parameter values",
                "complexity", "HIGH",
                "recommended", true
        ));

        methods.add(Map.of(
                "id", "RANDOM_SEARCH",
                "name", "Random Search",
                "description", "Random sampling of parameter combinations",
                "complexity", "MEDIUM",
                "recommended", false
        ));

        methods.add(Map.of(
                "id", "BAYESIAN",
                "name", "Bayesian Optimization",
                "description", "Probabilistic model-based optimization",
                "complexity", "MEDIUM",
                "recommended", true
        ));

        methods.add(Map.of(
                "id", "GENETIC",
                "name", "Genetic Algorithm",
                "description", "Evolution-inspired optimization",
                "complexity", "HIGH",
                "recommended", false
        ));

        methods.add(Map.of(
                "id", "WALK_FORWARD",
                "name", "Walk-Forward Analysis",
                "description", "Rolling window optimization with out-of-sample testing",
                "complexity", "VERY_HIGH",
                "recommended", true
        ));

        Map<String, Object> response = new HashMap<>();
        response.put("methods", methods);
        response.put("total", methods.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{optimizationId}/status")
    public ResponseEntity<Map<String, Object>> getOptimizationStatus(@PathVariable String optimizationId) {
        log.info("Getting optimization status: {}", optimizationId);

        return optimizations.stream()
                .filter(o -> optimizationId.equals(o.get("optimizationId")))
                .findFirst()
                .map(opt -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("optimizationId", optimizationId);
                    status.put("status", opt.getOrDefault("status", "PENDING"));
                    status.put("progress", opt.getOrDefault("progress", 0));
                    status.put("currentIteration", opt.getOrDefault("currentIteration", 0));
                    status.put("totalIterations", opt.getOrDefault("totalIterations", 100));
                    status.put("estimatedCompletion", opt.get("estimatedCompletion"));
                    status.put("message", "Optimization in progress");
                    return ResponseEntity.ok(status);
                })
                .orElseGet(() -> {
                    // Return mock status for non-existent optimization
                    Map<String, Object> status = new HashMap<>();
                    status.put("optimizationId", optimizationId);
                    status.put("status", "COMPLETED");
                    status.put("progress", 100);
                    status.put("currentIteration", 100);
                    status.put("totalIterations", 100);
                    status.put("message", "Optimization completed");
                    return ResponseEntity.ok(status);
                });
    }

    @PostMapping("/{optimizationId}/apply")
    public ResponseEntity<Map<String, Object>> applyOptimizationResult(
            @PathVariable String optimizationId,
            @RequestBody Map<String, Object> request) {
        log.info("Applying optimization result: optimizationId={}, request={}", optimizationId, request);

        String strategyId = (String) request.get("strategyId");

        Map<String, Object> result = new HashMap<>();
        result.put("optimizationId", optimizationId);
        result.put("strategyId", strategyId);
        result.put("applied", true);
        result.put("appliedAt", LocalDateTime.now());

        // Mock applied parameters
        Map<String, Object> appliedParams = new HashMap<>();
        appliedParams.put("stopLoss", 5);
        appliedParams.put("takeProfit", 10);
        appliedParams.put("positionSize", 1000000);
        result.put("appliedParameters", appliedParams);

        result.put("message", "Optimization parameters applied successfully to strategy " + strategyId);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{optimizationId}")
    public ResponseEntity<Void> deleteOptimization(@PathVariable String optimizationId) {
        log.info("Deleting optimization: {}", optimizationId);

        boolean removed = optimizations.removeIf(o -> optimizationId.equals(o.get("optimizationId")));

        if (removed) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{optimizationId}/results")
    public ResponseEntity<Map<String, Object>> getOptimizationResults(@PathVariable String optimizationId) {
        log.info("Getting optimization results: {}", optimizationId);

        Map<String, Object> results = new HashMap<>();
        results.put("optimizationId", optimizationId);
        results.put("status", "COMPLETED");
        results.put("completedAt", LocalDateTime.now());

        // Best parameters found
        Map<String, Object> bestParams = new HashMap<>();
        bestParams.put("stopLoss", 5);
        bestParams.put("takeProfit", 10);
        bestParams.put("positionSize", 1000000);
        results.put("bestParameters", bestParams);

        // Performance metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalReturn", new BigDecimal("15.5"));
        metrics.put("sharpeRatio", new BigDecimal("1.8"));
        metrics.put("maxDrawdown", new BigDecimal("8.2"));
        metrics.put("winRate", new BigDecimal("62.5"));
        results.put("metrics", metrics);

        // Iteration details
        results.put("totalIterations", 100);
        results.put("evaluatedCombinations", 100);
        results.put("executionTimeSeconds", 120);

        return ResponseEntity.ok(results);
    }
}
