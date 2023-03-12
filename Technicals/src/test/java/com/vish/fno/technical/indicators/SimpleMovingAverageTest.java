package com.vish.fno.technical.indicators;

import com.vish.fno.technical.model.Candle;
import com.vish.fno.technical.util.CandleUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public void testCalculateSMA() {
        List<Candle> candles = CandleUtils.getCandleData();
        List<Double> sma14 = underTest.calculate(candles);
        List<Double> expectedSMA14 = new ArrayList<>(Arrays.asList(2414.45d, 2417.925d, 2418.45d, 2417.9d, 2418.11d, 2418.425d, 2418.485714d, 2418.55d, 2418.527778d, 2418.74d, 2419.077273d, 2419.6875d, 2420.357692d, 2421.153571d, 2422.278571d, 2422.946429d, 2423.767857d, 2425.035714d, 2426.325d, 2427.432143d, 2428.939286d, 2430.510714d, 2432.260714d, 2433.814286d, 2434.964286d, 2435.917857d, 2436.739286d, 2437.353571d, 2437.982143d, 2438.603571d, 2439.385714d, 2440.053571d, 2440.435714d, 2440.971429d, 2441.203571d, 2441.485714d, 2441.496429d, 2441.507143d, 2441.682143d, 2441.6d, 2441.528571d, 2441.682143d, 2441.903571d, 2441.946429d, 2441.885714d, 2441.682143d, 2441.575d, 2441.403571d, 2441.028571d, 2440.471429d, 2439.917857d, 2439.442857d, 2439.1d, 2438.982143d, 2438.803571d, 2438.553571d, 2438.189286d, 2437.907143d, 2437.575d, 2437.271429d, 2436.853571d, 2436.417857d, 2436.096429d, 2435.939286d, 2435.992857d, 2436d, 2436.010714d, 2435.939286d, 2436.05d, 2435.992857d, 2436.057143d, 2436.335714d, 2436.489286d, 2436.685714d, 2437.007143d, 2437.296429d, 2437.514286d, 2437.589286d, 2437.657143d, 2437.642857d, 2437.578571d, 2437.5d, 2437.289286d, 2437.092857d, 2436.825d, 2436.332143d, 2436.185714d, 2435.871429d, 2435.475d, 2435.128571d, 2434.95d, 2434.807143d, 2434.585714d, 2434.4d, 2434.225d, 2434.096429d, 2433.796429d, 2433.453571d, 2433.192857d, 2432.992857d, 2432.625d, 2432.507143d, 2432.417857d, 2432.375d, 2432.1d, 2431.839286d, 2431.585714d, 2431.357143d, 2431.153571d, 2430.946429d, 2430.889286d, 2430.889286d, 2430.853571d, 2430.839286d, 2430.728571d, 2430.542857d, 2430.321429d, 2430.071429d, 2430.025d, 2430.139286d, 2430.178571d, 2430.203571d, 2430.207143d, 2430.285714d, 2430.464286d, 2430.646429d, 2430.842857d, 2430.978571d, 2431.060714d, 2431.128571d, 2431.157143d, 2431.339286d, 2431.553571d, 2431.621429d, 2431.757143d, 2431.910714d, 2432.078571d, 2432.064286d, 2432.007143d, 2431.921429d, 2431.839286d, 2431.839286d, 2431.889286d, 2431.967857d, 2432.1d, 2432.071429d, 2432.085714d, 2431.932143d, 2431.821429d, 2431.767857d, 2431.732143d, 2431.764286d, 2431.75d, 2431.825d, 2431.857143d, 2431.935714d, 2432.207143d, 2432.510714d, 2432.828571d, 2433.507143d, 2433.864286d, 2434.264286d, 2434.582143d, 2434.921429d, 2435.346429d, 2435.75d, 2436.153571d, 2436.525d, 2436.878571d, 2437.078571d, 2437.071429d, 2437.010714d, 2436.942857d, 2436.546429d, 2436.360714d, 2436.271429d, 2436.232143d, 2436.339286d, 2436.232143d, 2436.178571d, 2436.117857d, 2435.975d, 2435.946429d, 2435.953571d, 2436.039286d, 2436.053571d, 2436.125d, 2436.146429d, 2436.217857d, 2436.210714d, 2436.207143d, 2436.014286d, 2435.907143d, 2435.846429d, 2435.767857d, 2435.696429d, 2435.264286d, 2434.878571d, 2434.592857d, 2434.321429d, 2434.153571d, 2433.957143d, 2433.853571d, 2433.710714d, 2433.564286d, 2433.442857d, 2433.314286d, 2433.132143d, 2432.917857d, 2432.703571d, 2432.807143d, 2432.978571d, 2433.007143d, 2432.992857d, 2432.921429d, 2432.910714d, 2432.721429d, 2432.685714d, 2432.607143d, 2432.371429d, 2432.153571d, 2431.939286d, 2431.782143d, 2431.528571d, 2431.15d, 2430.607143d, 2430.107143d, 2429.732143d, 2429.278571d, 2428.682143d, 2428.335714d, 2427.814286d, 2427.492857d, 2427.153571d, 2426.892857d, 2426.660714d, 2426.367857d, 2426.125d, 2426.05d, 2426.078571d, 2425.996429d, 2425.796429d, 2425.85d, 2426.025d, 2426.085714d, 2426.217857d, 2426.125d, 2426.203571d, 2426.178571d, 2426.046429d, 2425.932143d, 2426.092857d, 2426.117857d, 2426.075d, 2426.125d, 2426.235714d, 2426.078571d, 2425.817857d, 2425.460714d, 2425.328571d, 2425.189286d, 2425.032143d, 2425.057143d, 2425.067857d, 2425.042857d, 2424.810714d, 2424.653571d, 2424.653571d, 2424.703571d, 2424.685714d, 2424.65d, 2424.739286d, 2424.935714d, 2424.935714d, 2425.096429d, 2425.285714d, 2425.539286d, 2425.892857d, 2426.278571d, 2426.625d, 2426.942857d, 2427.175d, 2427.357143d, 2427.614286d, 2427.903571d, 2428.242857d, 2428.542857d, 2428.885714d, 2429.142857d, 2429.403571d, 2429.460714d, 2429.532143d, 2429.675d, 2429.839286d, 2430.092857d, 2430.346429d, 2430.860714d, 2431.635714d, 2432.446429d, 2433.139286d, 2433.6d, 2434.085714d, 2434.521429d, 2434.964286d, 2435.385714d, 2435.789286d, 2436.167857d, 2436.442857d, 2436.707143d, 2436.971429d, 2436.910714d, 2436.503571d, 2436.142857d, 2435.910714d, 2435.828571d, 2435.717857d, 2435.657143d, 2435.610714d, 2435.575d, 2435.55d, 2435.45d, 2435.517857d, 2435.489286d, 2435.475d, 2435.55d, 2435.692857d, 2435.703571d, 2435.639286d, 2435.628571d, 2435.610714d, 2435.521429d, 2435.275d, 2435.071429d, 2434.614286d, 2434.257143d, 2433.907143d, 2433.671429d, 2433.417857d, 2433.075d, 2432.521429d, 2432.153571d, 2431.821429d, 2431.528571d, 2431.217857d, 2431.021429d, 2431.314286d, 2431.392857d, 2431.753571d, 2432.217857d, 2432.503571d, 2432.803571d, 2433.05d, 2433.378571d, 2433.917857d, 2434.507143d, 2435.175d, 2435.989286d, 2436.739286d, 2437.396429d, 2437.628571d, 2438.092857d, 2438.492857d, 2438.882143d, 2439.367857d, 2439.835714d, 2440.296429d, 2440.721429d, 2441.110714d, 2441.335714d, 2441.385714d, 2441.221429d, 2441.192857d, 2441.167857d, 2440.939286d, 2440.725d));
        for (int i = 0; i < sma14.size(); i++) {
            assertEquals(sma14.get(i), expectedSMA14.get(i), 0.001);
        }
    }
}