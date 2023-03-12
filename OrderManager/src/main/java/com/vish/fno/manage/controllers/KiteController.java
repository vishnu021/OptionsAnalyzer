package com.vish.fno.manage.controllers;

import com.vish.fno.manage.helper.DataCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class KiteController {
    private final DataCache dataCache;

//    @GetMapping("/allSymbols")
//    @ResponseStatus(HttpStatus.OK)
//    public Set<String> getAllSymbols() {
//        log.info("Returning all symbols");
//        return dataCache.getAllSymbols();
//    }

    @GetMapping("/exchangeSymbols/{exchange}")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> exchangeSymbol(@PathVariable String exchange) {
        log.info("Returning symbols for {} exchange", exchange);
        return dataCache.getFilteredSymbols(exchange);
    }

//    @GetMapping("/exchangeTokens/{exchange}")
//    @ResponseStatus(HttpStatus.OK)
//    public Map<String, Long> exchangeTokens(@PathVariable String exchange) {
//        log.info("Returning tokens for {} exchange", exchange);
//        return dataCache.getFilteredTokens(exchange);
//    }
}
