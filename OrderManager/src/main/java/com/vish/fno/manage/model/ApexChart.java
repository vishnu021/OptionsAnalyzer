package com.vish.fno.manage.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ApexChart {
    private List<ApexChartSeries> series;
    private List<ApexChartSeries> seriesBar;
}
