package com.vish.fno.manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class ApexChart {
    private List<ApexChartSeries> series;
    private List<ApexChartSeries> seriesBar;
}
