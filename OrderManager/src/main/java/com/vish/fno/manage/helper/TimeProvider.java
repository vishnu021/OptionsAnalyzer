package com.vish.fno.manage.helper;

import com.vish.fno.util.TimeUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

@Component
public class TimeProvider {

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    public int currentTimeStampIndex() {
        return TimeUtils.getIndexOfTimeStamp(new Date());
    }
}
