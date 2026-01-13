package maru.trading.domain.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Strategy version domain model.
 *
 * Represents a specific version of a strategy with parameters.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyVersion {

    private String strategyVersionId;
    private String strategyId;
    private Integer versionNo;
    private String paramsJson;

    /**
     * Parse parameters JSON to Map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getParamsAsMap() {
        if (paramsJson == null || paramsJson.isBlank()) {
            return Map.of();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(paramsJson, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse paramsJson: " + paramsJson, e);
        }
    }
}
