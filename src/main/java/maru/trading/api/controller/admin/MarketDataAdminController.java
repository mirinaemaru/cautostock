package maru.trading.api.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.AddSymbolsRequest;
import maru.trading.api.dto.request.RemoveSymbolsRequest;
import maru.trading.api.dto.response.AckResponse;
import maru.trading.api.dto.response.MarketDataStatusResponse;
import maru.trading.api.dto.response.SubscribedSymbolsResponse;
import maru.trading.application.service.MarketDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin controller for market data subscription management.
 *
 * Endpoints:
 * - POST /api/v1/admin/market-data/symbols - Add symbols to subscription
 * - DELETE /api/v1/admin/market-data/symbols - Remove symbols from subscription
 * - GET /api/v1/admin/market-data/symbols - Get subscribed symbols
 * - POST /api/v1/admin/market-data/resubscribe - Resubscribe to market data
 * - GET /api/v1/admin/market-data/status - Get subscription status
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/market-data")
@RequiredArgsConstructor
public class MarketDataAdminController {

    private final MarketDataService marketDataService;

    /**
     * Add new symbols to market data subscription.
     *
     * POST /api/v1/admin/market-data/symbols
     * Body: {"symbols": ["005490", "000270"]}
     *
     * This will dynamically add symbols without restarting the server.
     * The service will unsubscribe and resubscribe with all symbols (existing + new).
     */
    @PostMapping("/symbols")
    public ResponseEntity<AckResponse> addSymbols(@Valid @RequestBody AddSymbolsRequest request) {
        log.info("Adding symbols to subscription: {}", request.getSymbols());

        try {
            // Validate symbols
            for (String symbol : request.getSymbols()) {
                if (symbol == null || symbol.isBlank()) {
                    return ResponseEntity.badRequest()
                            .body(AckResponse.builder()
                                    .ok(false)
                                    .message("Invalid symbol: symbol cannot be null or blank")
                                    .build());
                }
            }

            // Add symbols through MarketDataService
            marketDataService.addSymbols(request.getSymbols());

            log.info("Successfully added {} symbols to subscription", request.getSymbols().size());

            return ResponseEntity.ok(AckResponse.builder()
                    .ok(true)
                    .message(String.format("Added %d symbols to subscription", request.getSymbols().size()))
                    .build());

        } catch (Exception e) {
            log.error("Failed to add symbols to subscription", e);
            return ResponseEntity.internalServerError()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Failed to add symbols: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Remove symbols from market data subscription.
     *
     * DELETE /api/v1/admin/market-data/symbols
     * Body: {"symbols": ["005380", "051910"]}
     *
     * This will dynamically remove symbols without restarting the server.
     * The service will unsubscribe and resubscribe with remaining symbols.
     */
    @DeleteMapping("/symbols")
    public ResponseEntity<AckResponse> removeSymbols(@Valid @RequestBody RemoveSymbolsRequest request) {
        log.info("Removing symbols from subscription: {}", request.getSymbols());

        try {
            // Validate symbols
            for (String symbol : request.getSymbols()) {
                if (symbol == null || symbol.isBlank()) {
                    return ResponseEntity.badRequest()
                            .body(AckResponse.builder()
                                    .ok(false)
                                    .message("Invalid symbol: symbol cannot be null or blank")
                                    .build());
                }
            }

            // Check if all symbols would be removed
            if (marketDataService.getSubscribedSymbols().size() == request.getSymbols().size()) {
                log.warn("Cannot remove all symbols - at least one symbol must remain subscribed");
                return ResponseEntity.badRequest()
                        .body(AckResponse.builder()
                                .ok(false)
                                .message("Cannot remove all symbols - at least one symbol must remain subscribed")
                                .build());
            }

            // Remove symbols through MarketDataService
            marketDataService.removeSymbols(request.getSymbols());

            log.info("Successfully removed {} symbols from subscription", request.getSymbols().size());

            return ResponseEntity.ok(AckResponse.builder()
                    .ok(true)
                    .message(String.format("Removed %d symbols from subscription", request.getSymbols().size()))
                    .build());

        } catch (Exception e) {
            log.error("Failed to remove symbols from subscription", e);
            return ResponseEntity.internalServerError()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Failed to remove symbols: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get currently subscribed symbols.
     *
     * GET /api/v1/admin/market-data/symbols
     *
     * Returns the list of symbols currently subscribed for real-time market data.
     */
    @GetMapping("/symbols")
    public ResponseEntity<SubscribedSymbolsResponse> getSubscribedSymbols() {
        log.debug("Getting subscribed symbols");

        try {
            List<String> symbols = new ArrayList<>(marketDataService.getSubscribedSymbols());
            String subscriptionId = marketDataService.getActiveSubscriptionId();
            boolean active = marketDataService.isSubscribed();

            SubscribedSymbolsResponse response = SubscribedSymbolsResponse.builder()
                    .symbols(symbols)
                    .total(symbols.size())
                    .subscriptionId(subscriptionId)
                    .active(active)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get subscribed symbols", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Resubscribe to market data.
     *
     * POST /api/v1/admin/market-data/resubscribe
     *
     * Useful after WebSocket reconnection or when subscription state is unclear.
     * This will resubscribe to all currently tracked symbols.
     */
    @PostMapping("/resubscribe")
    public ResponseEntity<AckResponse> resubscribe() {
        log.info("Resubscribing to market data");

        try {
            marketDataService.resubscribe();

            log.info("Successfully resubscribed to market data");

            return ResponseEntity.ok(AckResponse.builder()
                    .ok(true)
                    .message("Successfully resubscribed to market data")
                    .build());

        } catch (Exception e) {
            log.error("Failed to resubscribe to market data", e);
            return ResponseEntity.internalServerError()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Failed to resubscribe: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get market data subscription status.
     *
     * GET /api/v1/admin/market-data/status
     *
     * Returns the current status of market data subscription including:
     * - Whether subscribed
     * - Subscription ID
     * - Number of symbols
     * - Connection status
     */
    @GetMapping("/status")
    public ResponseEntity<MarketDataStatusResponse> getStatus() {
        log.debug("Getting market data subscription status");

        try {
            boolean subscribed = marketDataService.isSubscribed();
            String subscriptionId = marketDataService.getActiveSubscriptionId();
            int symbolCount = marketDataService.getSubscribedSymbols().size();

            String message;
            if (subscribed) {
                message = String.format("Active subscription with %d symbols", symbolCount);
            } else {
                message = "No active subscription";
            }

            MarketDataStatusResponse response = MarketDataStatusResponse.builder()
                    .subscribed(subscribed)
                    .subscriptionId(subscriptionId)
                    .symbolCount(symbolCount)
                    .connected(subscribed) // Connected if subscribed
                    .message(message)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get market data status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
