package com.vish.fno.manage.controllers;

import com.vish.fno.reader.service.KiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class KiteController {
    private final KiteService kiteService;

    @GetMapping("/allInstruments")
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, String>> allFilteredInstruments() {
        return kiteService.getFilteredInstruments();
    }
}