package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for list of fills.
 * Wraps collection of FillResponse objects.
 */
public class FillListResponse {

    @JsonProperty("items")
    private List<FillResponse> items;

    public FillListResponse() {
    }

    public FillListResponse(List<FillResponse> items) {
        this.items = items;
    }

    public List<FillResponse> getItems() {
        return items;
    }

    public void setItems(List<FillResponse> items) {
        this.items = items;
    }
}
