package maru.trading.application.usecase.trading;

import maru.trading.application.ports.repo.PositionRepository;
import maru.trading.application.ports.repo.RiskRuleRepository;
import maru.trading.application.ports.repo.RiskStateRepository;
import maru.trading.domain.execution.Position;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.domain.risk.RiskDecision;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskState;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluateRiskUseCase Test")
class EvaluateRiskUseCaseTest {

    @Mock
    private RiskStateRepository riskStateRepository;

    @Mock
    private RiskRuleRepository riskRuleRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private OrderJpaRepository orderRepository;

    @InjectMocks
    private EvaluateRiskUseCase evaluateRiskUseCase;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderId("ORDER_001")
                .accountId("ACC_001")
                .symbol("005930")
                .side(Side.BUY)
                .orderType(OrderType.MARKET)
                .qty(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(70000))
                .build();
    }

    @Test
    @DisplayName("Should approve order within risk limits")
    void shouldApproveOrderWithinRiskLimits() {
        // Given
        RiskRule defaultRule = RiskRule.defaultGlobalRule();
        RiskState defaultState = RiskState.defaultState();

        when(riskRuleRepository.findGlobalRule()).thenReturn(Optional.of(defaultRule));
        when(riskStateRepository.findByAccountId("ACC_001")).thenReturn(Optional.of(defaultState));
        when(orderRepository.countOpenOrdersByAccountId("ACC_001")).thenReturn(0L);
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930")).thenReturn(Optional.empty());

        // When
        RiskDecision decision = evaluateRiskUseCase.evaluate(testOrder);

        // Then
        assertThat(decision.isApproved()).isTrue();
    }

    @Test
    @DisplayName("Should use global state when account state not found")
    void shouldUseGlobalStateWhenAccountStateNotFound() {
        // Given
        RiskRule defaultRule = RiskRule.defaultGlobalRule();
        RiskState globalState = RiskState.defaultState();

        when(riskRuleRepository.findGlobalRule()).thenReturn(Optional.of(defaultRule));
        when(riskStateRepository.findByAccountId("ACC_001")).thenReturn(Optional.empty());
        when(riskStateRepository.findGlobalState()).thenReturn(Optional.of(globalState));
        when(orderRepository.countOpenOrdersByAccountId("ACC_001")).thenReturn(0L);
        when(positionRepository.findByAccountAndSymbol(any(), any())).thenReturn(Optional.empty());

        // When
        RiskDecision decision = evaluateRiskUseCase.evaluate(testOrder);

        // Then
        assertThat(decision).isNotNull();
        verify(riskStateRepository).findGlobalState();
    }

    @Test
    @DisplayName("Should use default rule when no rule found")
    void shouldUseDefaultRuleWhenNoRuleFound() {
        // Given
        when(riskRuleRepository.findGlobalRule()).thenReturn(Optional.empty());
        when(riskStateRepository.findByAccountId("ACC_001")).thenReturn(Optional.of(RiskState.defaultState()));
        when(orderRepository.countOpenOrdersByAccountId("ACC_001")).thenReturn(0L);
        when(positionRepository.findByAccountAndSymbol(any(), any())).thenReturn(Optional.empty());

        // When
        RiskDecision decision = evaluateRiskUseCase.evaluate(testOrder);

        // Then
        assertThat(decision).isNotNull();
    }

    @Test
    @DisplayName("Should consider existing position in risk evaluation")
    void shouldConsiderExistingPositionInRiskEvaluation() {
        // Given
        Position existingPosition = new Position(
                "POS_001",
                "ACC_001",
                "005930",
                100,
                BigDecimal.valueOf(68000),
                BigDecimal.ZERO
        );

        when(riskRuleRepository.findGlobalRule()).thenReturn(Optional.of(RiskRule.defaultGlobalRule()));
        when(riskStateRepository.findByAccountId("ACC_001")).thenReturn(Optional.of(RiskState.defaultState()));
        when(orderRepository.countOpenOrdersByAccountId("ACC_001")).thenReturn(0L);
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
                .thenReturn(Optional.of(existingPosition));

        // When
        RiskDecision decision = evaluateRiskUseCase.evaluate(testOrder);

        // Then
        assertThat(decision).isNotNull();
        verify(positionRepository).findByAccountAndSymbol("ACC_001", "005930");
    }
}
