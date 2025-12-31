package maru.trading.api.controller.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.response.OrderResponse;
import maru.trading.domain.order.OrderNotFoundException;
import maru.trading.domain.order.OrderStatus;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주문 조회 Query Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/query/orders")
@RequiredArgsConstructor
public class OrderQueryController {

	private final OrderJpaRepository orderRepository;

	/**
	 * 주문 검색
	 */
	@GetMapping
	public ResponseEntity<Map<String, List<OrderResponse>>> searchOrders(
			@RequestParam(required = false) String accountId,
			@RequestParam(required = false) String symbol,
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@RequestParam(defaultValue = "50") Integer limit
	) {
		List<OrderEntity> orders;

		if (accountId != null && from != null && to != null) {
			orders = orderRepository.findByAccountIdAndCreatedAtBetween(accountId, from, to);
		} else if (symbol != null && from != null && to != null) {
			orders = orderRepository.findBySymbolAndCreatedAtBetween(symbol, from, to);
		} else if (accountId != null && status != null) {
			orders = orderRepository.findByAccountIdAndStatus(accountId, status);
		} else {
			orders = orderRepository.findAll();
		}

		// Limit 적용
		List<OrderResponse> items = orders.stream()
				.limit(limit)
				.map(this::toResponse)
				.collect(Collectors.toList());

		Map<String, List<OrderResponse>> response = new HashMap<>();
		response.put("items", items);

		return ResponseEntity.ok(response);
	}

	/**
	 * 주문 조회
	 */
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));

		return ResponseEntity.ok(toResponse(order));
	}

	private OrderResponse toResponse(OrderEntity entity) {
		return OrderResponse.builder()
				.orderId(entity.getOrderId())
				.accountId(entity.getAccountId())
				.strategyId(entity.getStrategyId())
				.strategyVersionId(entity.getStrategyVersionId())
				.symbol(entity.getSymbol())
				.side(entity.getSide())
				.orderType(entity.getOrderType())
				.ordDvsn(entity.getOrdDvsn())
				.qty(entity.getQty())
				.price(entity.getPrice())
				.status(entity.getStatus())
				.idempotencyKey(entity.getIdempotencyKey())
				.brokerOrderNo(entity.getBrokerOrderNo())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}
