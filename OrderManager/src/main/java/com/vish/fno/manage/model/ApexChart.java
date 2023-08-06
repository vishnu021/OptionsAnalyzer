package com.vish.fno.manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class ApexChart {
    private String date;
    private String symbol;
    private String instrument;
    private int dataPoints;
    private List<ApexChartSeries> series;
    private List<ApexChartSeries> seriesBar;
}
