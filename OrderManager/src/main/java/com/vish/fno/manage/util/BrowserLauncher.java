package com.vish.fno.manage.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@AllArgsConstructor
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class BrowserLauncher {

    private final boolean liveData;
    private final String authenticationUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void launchBrowser() {
        if (liveData) {
            log.info("Initialising browser");
            System.setProperty("java.awt.headless", "false");
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(authenticationUrl));
            } catch (IOException | URISyntaxException e) {
                log.error("Exception while launching browser", e);
                throw new RuntimeException(e);
            }
        }
    }
}
