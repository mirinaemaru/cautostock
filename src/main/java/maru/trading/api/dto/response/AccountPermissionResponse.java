package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Account Permission Response
 */
@Getter
@Builder
public class AccountPermissionResponse {

    /**
     * 계좌 ID
     */
    private final String accountId;

    /**
     * 매수 허용
     */
    private final Boolean tradeBuy;

    /**
     * 매도 허용
     */
    private final Boolean tradeSell;

    /**
     * 자동매매 허용
     */
    private final Boolean autoTrade;

    /**
     * 수동매매 허용
     */
    private final Boolean manualTrade;

    /**
     * PAPER 전용
     */
    private final Boolean paperOnly;

    /**
     * 수정 시각
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime updatedAt;
}
