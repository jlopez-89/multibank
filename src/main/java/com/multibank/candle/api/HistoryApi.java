package com.multibank.candle.api;

import com.multibank.candle.api.dto.HistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "Candles",
        description = "Endpoints for querying OHLC candles for any supported symbol and timeframe."
)
public interface HistoryApi {

    @Operation(
            summary = "Get candle history",
            description = """
                    Returns OHLC candles for a symbol and timeframe within the [from, to] range.
                    The result is sorted by time ascending.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Candles successfully retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = HistoryResponse.class)))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid parameters: e.g., from >= to",
            content = @Content(schema = @Schema(example = """
                        { "error": "Invalid time range: 'from' must be less than 'to'" }
                    """))
    )
    @ApiResponse(
            responseCode = "404",
            description = "No candles found for the given symbol/timeframe in range"
    )
    @Parameters({
            @Parameter(
                    name = "symbol",
                    description = "Trading symbol (e.g. BTC-USD, ETH-USD)",
                    required = true,
                    example = "BTC-USD"
            ),
            @Parameter(
                    name = "interval",
                    description = "Candle timeframe (1s, 1m, 5m, etc.)",
                    required = true,
                    example = "1m"
            ),
            @Parameter(
                    name = "from",
                    description = "Start timestamp (epoch seconds). Inclusive.",
                    required = true,
                    example = "1700000000"
            ),
            @Parameter(
                    name = "to",
                    description = "End timestamp (epoch seconds). Must be > from.",
                    required = true,
                    example = "1700000600"
            )
    })
    HistoryResponse getHistory(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam long from,
            @RequestParam long to
    );

}
