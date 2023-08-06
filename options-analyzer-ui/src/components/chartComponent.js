import React from 'react';
import ReactApexChart from "react-apexcharts";
import {candleStickPx, volumeBarPx} from "../services/windowHeightService";

function ChartComponent({series, options, seriesBar, optionsBar}) {
    return (
        <div className="chart-box">
            <div id="chart-candlestick">
                <ReactApexChart options={options} series={series} type="candlestick" height={candleStickPx(window.innerHeight)} width="100%" />
            </div>
            <div id="chart-bar">
                <ReactApexChart options={optionsBar} series={seriesBar} type="bar" height={volumeBarPx(window.innerHeight)} width="100%" />
            </div>
        </div>
    );
}

export default ChartComponent;
