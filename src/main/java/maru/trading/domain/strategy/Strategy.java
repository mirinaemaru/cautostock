package maru.trading.domain.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.shared.Environment;

/**
 * Strategy domain model.
 *
 * Represents a trading strategy configuration.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Strategy {

    private String strategyId;
    private String name;
    private String description;
    private String status; // ACTIVE, INACTIVE
    private Environment mode; // PAPER, LIVE
    private String activeVersionId;

    /**
     * Check if strategy is active.
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * Activate strategy with a specific version.
     */
    public void activate(String versionId) {
        this.status = "ACTIVE";
        this.activeVersionId = versionId;
    }

    /**
     * Deactivate strategy.
     */
    public void deactivate() {
        this.status = "INACTIVE";
    }
}
