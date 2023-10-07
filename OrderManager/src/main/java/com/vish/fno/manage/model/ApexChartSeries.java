package com.vish.fno.manage.model;

import com.vish.fno.model.Candle;
import com.vish.fno.model.SymbolData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Data
public class ApexChartSeries {
    private static final String FULL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final SimpleDateFormat formatterMilliSecond = new SimpleDateFormat(FULL_DATE_FORMAT);
    private String type;
    private String name;
    private List<? extends DataField> data;

    public ApexChartSeries(String name, String type, SymbolData symbolData) {
        this.name = name;
        this.type = type;
        this.data = symbolData.getData().stream().map(ChartData::new).toList();
    }

    public ApexChartSeries(String name, String type, List<Double> ema, List<Candle> candles) {
        this.name = name;
        this.type = type;
        this.data = IntStream.range(0, candles.size()).boxed().map(i -> new LineData(ema.get(i), candles.get(i))).toList();
    }

    @Data
    private abstract static class DataField {
        protected long x;
    }

    @Data
    @EqualsAndHashCode(callSuper=false)
    private static class ChartData extends DataField {
        protected List<Double> y;

        public ChartData(Candle candle) {
            try {
                this.x = formatterMilliSecond.parse(candle.getTime()).getTime();
                this.y = List.of(candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose());
            } catch (ParseException e) {
                log.error("Failed to parse dateTime value");
            }
        }
    }

    @Data
    @EqualsAndHashCode(callSuper=false)
    private static class LineData extends DataField {
        protected Double y;
        public LineData(Double values, Candle candle) {
            try {
                this.x = formatterMilliSecond.parse(candle.getTime()).getTime();
                this.y = values;
            } catch (ParseException e) {
                log.error("Failed to parse dateTime value");
            }
        }
    }
}
