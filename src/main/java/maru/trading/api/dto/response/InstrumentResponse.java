package maru.trading.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for instrument data.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentResponse {

    private String symbol;
    private String market;
    private String nameKr;
    private String nameEn;
    private String sectorCode;
    private String industry;
    private BigDecimal tickSize;
    private Integer lotSize;
    private LocalDate listingDate;
    private LocalDate delistingDate;
    private String status;
    private Boolean tradable;
    private Boolean halted;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
