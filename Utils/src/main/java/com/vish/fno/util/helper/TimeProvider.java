package com.vish.fno.util.helper;

import com.vish.fno.util.TimeUtils;

import java.time.LocalDateTime;
import java.util.Date;

public class TimeProvider {

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    public Date todayDate() {
        return new Date();
    }

    public int currentTimeStampIndex() {
        return TimeUtils.getIndexOfTimeStamp(new Date());
    }
}
