package com.vish.fno.manage.controllers;

import com.vish.fno.manage.helper.DataCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class KiteController {
    private final DataCache dataCache;

    @GetMapping("/exchangeSymbols")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> exchangeSymbol() {
        return dataCache.getFilteredSymbols();
    }

    @GetMapping("/allInstruments")
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, String>> allFilteredInstruments() {
        return dataCache.getAllInstruments();
    }

    @GetMapping("/expiryDates")
    @ResponseStatus(HttpStatus.OK)
    public Set<String> expiryDates() {
        return dataCache.getExpiryDates();
    }
}
