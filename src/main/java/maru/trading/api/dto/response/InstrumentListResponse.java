package maru.trading.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for instrument list.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentListResponse {

    private List<InstrumentResponse> items;
    private Integer total;
}
