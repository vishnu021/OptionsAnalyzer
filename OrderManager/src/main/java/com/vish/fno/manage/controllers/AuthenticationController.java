package com.vish.fno.manage.controllers;

import com.vish.fno.reader.service.KiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthenticationController {

    private final KiteService kiteService;

    @GetMapping("/authenticate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> authenticate(@RequestParam String request_token,
                                               @RequestParam String status,
                                               @RequestParam String action) {

        log.info("Got response token, status: {} and action: {}", status, action);
        if (kiteService.isInitialised()) {
            return new ResponseEntity<>("Already initialised", HttpStatus.ALREADY_REPORTED);
        }

        kiteService.authenticate(request_token);
        return ResponseEntity.ok("Authenticated");
    }
}
