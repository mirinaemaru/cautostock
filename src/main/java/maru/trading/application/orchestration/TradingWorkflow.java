package maru.trading.application.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.application.usecase.trading.PlaceOrderUseCase;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalType;
import maru.trading.infra.config.UlidGenerator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Trading Workflow (신호 → 리스크 → 주문)
 *
 * 신호를 받아 리스크 평가 후 주문을 생성하는 전체 흐름을 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingWorkflow {

	private final PlaceOrderUseCase placeOrderUseCase;

	/**
	 * 신호 처리
	 */
	public void processSignal(Signal signal) {
		log.info("Process signal: signalId={}, type={}, symbol={}",
				signal.getSignalId(), signal.getSignalType(), signal.getSymbol());

		// HOLD 신호는 무시
		if (signal.getSignalType() == SignalType.HOLD) {
			log.debug("Signal type is HOLD, skipping: reason={}", signal.getReason());
			return;
		}

		// 주문 생성
		Order order = createOrderFromSignal(signal);

		// 주문 실행 (리스크 평가 포함)
		try {
			Order executedOrder = placeOrderUseCase.execute(order);
			log.info("Order executed: orderId={}, status={}",
					executedOrder.getOrderId(), executedOrder.getStatus());

		} catch (Exception e) {
			log.error("Failed to execute order from signal: signalId={}",
					signal.getSignalId(), e);
		}
	}

	/**
	 * 신호로부터 주문 생성
	 */
	private Order createOrderFromSignal(Signal signal) {
		String orderId = UlidGenerator.generate();
		String idempotencyKey = "sig_" + signal.getSignalId();

		// 신호 타입에 따라 매수/매도 결정
		Side side = signal.getSignalType() == SignalType.BUY ? Side.BUY : Side.SELL;

		// 목표값을 수량으로 변환 (여기서는 간단히 그대로 사용)
		BigDecimal qty = signal.getTargetValue();

		// MVP: 시장가 주문으로 처리
		OrderType orderType = OrderType.MARKET;
		String ordDvsn = "01"; // KIS: 01=시장가

		return Order.builder()
				.orderId(orderId)
				.accountId(signal.getAccountId())
				.strategyId(signal.getStrategyId())
				.strategyVersionId(signal.getStrategyVersionId())
				.signalId(signal.getSignalId())
				.symbol(signal.getSymbol())
				.side(side)
				.orderType(orderType)
				.ordDvsn(ordDvsn)
				.qty(qty)
				.price(null) // 시장가는 가격 없음
				.idempotencyKey(idempotencyKey)
				.build();
	}
}
