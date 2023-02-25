package com.vish.fno.manage.util;

import com.vish.fno.reader.service.KiteService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@AllArgsConstructor
public class ConsoleAuthenticator {

    @Autowired
    private KiteService kiteService;

    private final String authenticationUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void authenticateFromConsole() {
        log.info("Navigate to following url to get the authentication token");
        log.info("url : {}", authenticationUrl);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String authenticationToken = br.readLine();
            kiteService.authenticate(authenticationToken);
        } catch (Exception e) {
                log.error("Exception while launching browser",e);
        }
    }
}
