package com.vish.fno.technical.indicators;

import com.vish.fno.technical.indicators.ma.ExponentialMovingAverage;
import com.vish.fno.technical.model.Candle;
import com.vish.fno.technical.util.CandleUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static com.vish.fno.technical.util.CandleUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ExponentialMovingAverageTest {
    private static final int DURATION = 14;
    private ExponentialMovingAverage underTest;

    @BeforeEach
    public void before() {
        MockitoAnnotations.openMocks(this);
        underTest = new ExponentialMovingAverage(DURATION);
    }

    @Test
    public void testCalculateEMAWithLastEma() {
        //Given
        List<Candle> candles = CandleUtils.getCandleData();
        List<Candle> prevDayCandles = CandleUtils.getPrevDayCandleData();
        List<Double> expectedEMA14 = getEmaData("ema14withPrevEma.txt");
        // When
        List<Double> actualEma = underTest.calculate(candles, prevDayCandles);
        // Then
        for (int i = 0; i < actualEma.size(); i++) {
            assertEquals(actualEma.get(i), expectedEMA14.get(i), 0.001);
        }
    }

    @Test
    public void testCalculateEMA() {
        //Given
        List<Candle> candles = CandleUtils.getCandleData();
        List<Double> expectedEMA14 = getEmaData("ema14.txt");
        // When
        List<Double> ema14 = underTest.calculate(candles);
        // Then
        for (int i = 0; i < ema14.size(); i++) {
            assertEquals(ema14.get(i), expectedEMA14.get(i), 0.001);
        }
    }
}