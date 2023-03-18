package com.vish.fno.technical.indicators;

import com.vish.fno.technical.indicators.ma.SimpleMovingAverage;
import com.vish.fno.technical.model.Candle;
import com.vish.fno.technical.util.CandleUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static com.vish.fno.technical.util.CandleUtils.getSmaData;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class SimpleMovingAverageTest {
    private static final int DURATION = 14;
    private SimpleMovingAverage underTest;

    @BeforeEach
    public void before() {
        MockitoAnnotations.openMocks(this);
        underTest = new SimpleMovingAverage(DURATION);
    }

    @Test
    public void testCalculateSMAWithLastSMA() {
        //Given
        List<Candle> candles = CandleUtils.getCandleData();
        List<Candle> prevDayCandles = CandleUtils.getPrevDayCandleData();
        List<Double> expectedEMA14 = getSmaData("sma14withPrevSMA.txt");
        // When
        List<Double> actualSMA = underTest.calculate(candles, prevDayCandles);
        // Then
        for (int i = 0; i < actualSMA.size(); i++) {
            assertEquals(actualSMA.get(i), expectedEMA14.get(i), 0.001);
        }
    }

    @Test
    public void testCalculateSMA() {
        //Given
        List<Candle> candles = CandleUtils.getCandleData();
        List<Double> expectedSMA14 = getSmaData("sma14.txt");
        // When
        List<Double> sma14 = underTest.calculate(candles);
        // Then
        for (int i = 0; i < sma14.size(); i++) {
            assertEquals(sma14.get(i), expectedSMA14.get(i), 0.001);
        }
    }
}