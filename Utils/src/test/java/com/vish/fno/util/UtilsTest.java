package com.vish.fno.util;

import org.junit.jupiter.api.Test;

import static com.vish.fno.util.Utils.round;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    @Test
    void roundToNearestFiveCentsTest() {
        double originalValue = 10.03;
        double actualValue = round(originalValue);
        double expectedValue = 10.05;
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void roundToNearestFiveCentsTest2() {
        double originalValue = 0.01;
        double actualValue = round(originalValue);
        double expectedValue = 0.00;
        assertEquals(expectedValue, actualValue);
    }
}