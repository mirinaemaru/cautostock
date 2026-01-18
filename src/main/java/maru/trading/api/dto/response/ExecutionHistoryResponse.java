package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.infra.persistence.jpa.entity.ExecutionHistoryEntity;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionHistoryResponse {

    private String executionId;
    private String strategyId;
    private String accountId;
    private String executionType;
    private String status;
    private String symbol;
    private String description;
    private String details;
    private String errorMessage;
    private Integer executionTimeMs;
    private LocalDateTime createdAt;

    public static ExecutionHistoryResponse fromEntity(ExecutionHistoryEntity entity) {
        return ExecutionHistoryResponse.builder()
                .executionId(entity.getExecutionId())
                .strategyId(entity.getStrategyId())
                .accountId(entity.getAccountId())
                .executionType(entity.getExecutionType())
                .status(entity.getStatus())
                .symbol(entity.getSymbol())
                .description(entity.getDescription())
                .details(entity.getDetails())
                .errorMessage(entity.getErrorMessage())
                .executionTimeMs(entity.getExecutionTimeMs())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutionHistoryList {
        private List<ExecutionHistoryResponse> executions;
        private int totalCount;
        private int page;
        private int pageSize;
        private long successCount;
        private long failedCount;
    }
}
