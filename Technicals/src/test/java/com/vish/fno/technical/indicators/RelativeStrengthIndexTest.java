package com.vish.fno.technical.indicators;

import com.vish.fno.model.Candle;
import com.vish.fno.technical.util.CandleUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static com.vish.fno.technical.util.CandleUtils.getEmaData;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class RelativeStrengthIndexTest {
    private static final int DURATION = 14;
    private RelativeStrengthIndex underTest;

    @BeforeEach
    public void before() {
        MockitoAnnotations.openMocks(this);
        underTest = new RelativeStrengthIndex(DURATION);
    }

    @Test
    public void testCalculateRSI() {
        //Given
        List<Candle> candles = CandleUtils.getCandleData();
        List<Double> expectedRSI14 = getEmaData("rsi14.txt");
        // When
        List<Double> rsi = underTest.calculate(candles);
        // Then
        for (int i = 0; i < rsi.size(); i++) {
            assertEquals(rsi.get(i), expectedRSI14.get(i), 0.001);
        }
    }

    @Test
    public void testCalculateRSIfromPrevData() {
        //Given
        List<Candle> candles = CandleUtils.getCandleData();
        List<Candle> prevDayCandles = CandleUtils.getPrevDayCandleData();
        List<Double> expectedRSI14 = getEmaData("rsi14withPrevRSI.txt");
        // When
        List<Double> rsi = underTest.calculate(candles, prevDayCandles);
        // Then
        for (int i = 0; i < rsi.size(); i++) {
            assertEquals(rsi.get(i), expectedRSI14.get(i), 0.001);
        }
    }
}