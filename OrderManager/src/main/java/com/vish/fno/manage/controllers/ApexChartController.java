package com.vish.fno.manage.controllers;

import com.vish.fno.manage.model.ApexChart;
import com.vish.fno.manage.service.CandlestickService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.vish.fno.manage.controllers.CandlestickController.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApexChartController {

    private final CandlestickService candlestickService;

    @GetMapping("/apexChart/{date}/{symbol}")
    @Operation(summary = "Returns the apex chart candle data for 1 minute interval")
    public ResponseEntity<ApexChart> apexChart(
            @Parameter(description = DATE_DESCRIPTION, required = true)
            @PathVariable String date,
            @Parameter(description = SYMBOL_DESCRIPTION, required = true)
            @PathVariable String symbol) {
        return candlestickService.getEntireDayHistoryApexChart(date, symbol)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    @GetMapping("/apexChart/{date}/{symbol}/{interval}")
    @Operation(summary = "Returns the apex chart candle data for 1 minute interval")
    public ResponseEntity<ApexChart> apexChartWithInterval(
            @Parameter(description = DATE_DESCRIPTION, required = true)
            @PathVariable String date,
            @Parameter(description = SYMBOL_DESCRIPTION, required = true)
            @PathVariable String symbol,
            @Parameter(description = INTERVAL_DESCRIPTION, required = true)
            @PathVariable String interval) {
        return candlestickService.getEntireDayHistoryApexChart(date, symbol, interval)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }
}
