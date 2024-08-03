package com.vish.fno.manage.helper;

import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.helper.TimeProvider;

public abstract class AbstractEntryVerifier implements EntryVerifier {
    public static final String ORDER_EXECUTED = "orderExecuted";

    protected final KiteService kiteService;
    protected final TimeProvider timeProvider;

    public AbstractEntryVerifier(KiteService kiteService, TimeProvider timeProvider) {
        this.kiteService = kiteService;
        this.timeProvider = timeProvider;
    }
}
