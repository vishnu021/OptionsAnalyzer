package com.vish.fno.manage.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.*;
import java.net.URI;

@Slf4j
@AllArgsConstructor
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
            } catch (Exception e) {
                log.error("Exception while launching browser",e);
            }
        }
    }
}
