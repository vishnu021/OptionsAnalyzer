package com.vish.fno.manage.controllers;

import com.vish.fno.manage.model.ApexChart;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.model.SymbolData;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CandlestickController {

    private final CandlestickService candlestickService;

    private static final String INTERVAL_DESCRIPTION = "interval of data (minute|3minute|5minute|10minute|15minute|30minute|60minute|day)";
    private static final String DATE_DESCRIPTION = "date in yyyy-MM-dd format";
    private static final String SYMBOL_DESCRIPTION = "Symbol of the Company or Index";

    @GetMapping("/historicalData/{date}/{symbol}")
    @ApiOperation(value = "Returns the historical candle data for 1 minute interval")
    public ResponseEntity<SymbolData> historicalData(@ApiParam(DATE_DESCRIPTION) @PathVariable String date,
                                                     @ApiParam(SYMBOL_DESCRIPTION) @PathVariable String symbol) {
        SymbolData historicalDataList = candlestickService.getEntireDayHistoryData(date, symbol);

        if(historicalDataList==null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        return ResponseEntity.status(HttpStatus.OK).body(historicalDataList);
    }

    @GetMapping("/historicalData/{date}/{symbol}/{interval}")
    @ApiOperation(value = "Returns the historical candle data for specified interval")
    public ResponseEntity<SymbolData> historicalData(@ApiParam(DATE_DESCRIPTION) @PathVariable String date,
                                                     @ApiParam(SYMBOL_DESCRIPTION) @PathVariable String symbol,
                                                     @ApiParam(INTERVAL_DESCRIPTION) @PathVariable String interval) {
        SymbolData historicalDataList = candlestickService.getEntireDayHistoryData(date, symbol, interval);

        if(historicalDataList==null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        return ResponseEntity.status(HttpStatus.OK).body(historicalDataList);
    }

    @GetMapping("/apexChart/{date}/{symbol}")
    @ApiOperation(value = "Returns the apex chart candle data for 1 minute interval")
    public ResponseEntity<ApexChart> apexChart(@ApiParam(DATE_DESCRIPTION) @PathVariable String date,
                                               @ApiParam(SYMBOL_DESCRIPTION) @PathVariable String symbol) {
        ApexChart apexChart = candlestickService.getEntireDayHistoryApexChart(date, symbol);

        if(apexChart==null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        return ResponseEntity.status(HttpStatus.OK).body(apexChart);
    }
}
