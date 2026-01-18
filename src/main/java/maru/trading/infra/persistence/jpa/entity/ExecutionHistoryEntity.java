package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionHistoryEntity {

    @Id
    @Column(name = "execution_id", columnDefinition = "CHAR(26)")
    private String executionId;

    @Column(name = "strategy_id", columnDefinition = "CHAR(26)", nullable = false)
    private String strategyId;

    @Column(name = "account_id", columnDefinition = "CHAR(26)")
    private String accountId;

    @Column(name = "execution_type", length = 32, nullable = false)
    private String executionType;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "symbol", length = 16)
    private String symbol;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "details", columnDefinition = "JSON")
    private String details;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
