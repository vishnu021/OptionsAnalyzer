package com.vish.fno.manage.controllers;

import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.model.Candle;
import com.vish.fno.model.SymbolData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CandlestickController {

    private final CandlestickService candlestickService;

    public static final String INTERVAL_DESCRIPTION = "interval of data (minute|3minute|5minute|10minute|15minute|30minute|60minute|day)";
    public static final String DATE_DESCRIPTION = "date in yyyy-MM-dd format";
    public static final String SYMBOL_DESCRIPTION = "Symbol of the Company or Index";

    @GetMapping("/historicalData/{date}/{symbol}")
    @Operation(summary = "Returns the historical candle data for 1 minute interval")
    public ResponseEntity<SymbolData> historicalData(
            @Parameter(description = DATE_DESCRIPTION, required = true)
            @PathVariable String date,
            @Parameter(description = SYMBOL_DESCRIPTION, required = true)
            @PathVariable String symbol) {
        return candlestickService.getEntireDayHistoryData(date, symbol)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
    }

    @GetMapping("/historicalData/{date}/{symbol}/{interval}")
    @Operation(summary = "Returns the historical candle data for specified interval")
    public ResponseEntity<SymbolData> historicalData(
            @Parameter(description = DATE_DESCRIPTION, required = true)
            @PathVariable String date,
            @Parameter(description = SYMBOL_DESCRIPTION, required = true)
            @PathVariable String symbol,
            @Parameter(description = INTERVAL_DESCRIPTION, required = true)
            @PathVariable String interval) {
        return candlestickService.getEntireDayHistoryData(date, symbol, interval)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    @GetMapping("/multiDayHistoricalData/{from}/{to}/{symbol}/{interval}")
    @Operation(summary = "Returns the historical candle data for specified interval")
    public ResponseEntity<List<Candle>> multiDayHistoricalData(
            @Parameter(description = DATE_DESCRIPTION, required = true)
            @PathVariable String from,
            @Parameter(description = DATE_DESCRIPTION, required = true)
            @PathVariable String to,
            @Parameter(description = SYMBOL_DESCRIPTION, required = true)
            @PathVariable String symbol,
            @Parameter(description = INTERVAL_DESCRIPTION, required = true)
            @PathVariable String interval) {
        return candlestickService.getHistoryData(from, to, symbol, interval)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }
}
