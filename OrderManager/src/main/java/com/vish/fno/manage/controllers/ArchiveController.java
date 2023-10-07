package com.vish.fno.manage.controllers;

import com.vish.fno.manage.util.ArchiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiverService archiverService;

    @GetMapping("/archiver")
    public ResponseEntity<String> archive() {
        archiverService.getAllNiftyOptions();
        return ResponseEntity.ok("Done");
    }
}
