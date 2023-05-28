import React from 'react';
import ReactApexChart from "react-apexcharts";

function ChartComponent({series, options, seriesBar, optionsBar}) {
    return (
        <div className="chart-box" style={{ zIndex: 0 }}>
            <div id="chart-candlestick">
                <ReactApexChart options={options} series={series} type="candlestick" height={600} width="100%" />
            </div>
            <div id="chart-bar">
                <ReactApexChart options={optionsBar} series={seriesBar} type="bar" height={150} width="100%" />
            </div>
        </div>
    );
}

export default ChartComponent;
