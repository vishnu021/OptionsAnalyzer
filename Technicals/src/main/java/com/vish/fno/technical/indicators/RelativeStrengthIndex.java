package com.vish.fno.technical.indicators;

import com.vish.fno.technical.model.Candlestick;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor
public class RelativeStrengthIndex implements Indicator {


    private int duration = 14;
    private List<Double> avgU;
    private List<Double> avgD;
    @Getter
    private List<Double> rsi;

    public RelativeStrengthIndex(int duration) {
        this.duration = duration;
    }

    public List<Double> calculateRsi(List<Candlestick> candles) {
        avgU = new ArrayList<>();
        avgD = new ArrayList<>();
        rsi = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            avgU.add(calculateAvgMovememnt(candles.get(i).getClose() - candles.get(i - 1).getClose(), avgU, 14));
            avgD.add(calculateAvgMovememnt(candles.get(i - 1).getClose() - candles.get(i).getClose(), avgD, 14));
            double rsiValue = getRsiFromAvg(avgU.get(avgU.size() - 1), avgD.get(avgD.size() - 1));
            rsi.add(((double) Math.round(rsiValue * 100)) / 100);
        }
        return rsi;
    }

    @Override
    public List<Double> calculate(List<Candlestick> candles) {
        avgU = new ArrayList<>();
        avgD = new ArrayList<>();
        rsi = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            avgU.add(calculateAvgMovememnt(candles.get(i).getClose() - candles.get(i - 1).getClose(), avgU, duration));
            avgD.add(calculateAvgMovememnt(candles.get(i - 1).getClose() - candles.get(i).getClose(), avgD, duration));
            double rsiValue = getRsiFromAvg(avgU.get(avgU.size() - 1), avgD.get(avgD.size() - 1));
            rsi.add(((double) Math.round(rsiValue * 100)) / 100);
        }
        return rsi;
    }

    private double calculateAvgMovememnt(double closeDifference, List<Double> avg, int n) {
        closeDifference = closeDifference > 0 ? closeDifference : 0;
        int avgSize = avg.size();
        if (avgSize == 0) {
            return closeDifference;
        }
        double previousAvg = avg.get(avgSize - 1);

        if (avgSize < n) {
            return (previousAvg * (avgSize) + closeDifference) / (avgSize + 1);
        }
        return (closeDifference - previousAvg) * 2 / (n + 1) + previousAvg;
    }

    private double getRsiFromAvg(double avgU, double avgD) {
        if (avgD == 0d)
            return 100;
        double rs = avgU / avgD;
        double rsi = 100 - 100 / (1 + rs);
        return ((double) Math.round(rsi * 100)) / 100;
    }
}

