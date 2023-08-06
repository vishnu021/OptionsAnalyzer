import {volumeBarPx} from "./windowHeightService";

export const getBarOptions = () => {

    const getXAxisOptions = () => {
        return {
            type: 'datetime',
            labels: {
                datetimeUTC: false,
                formatter: (value) => {
                    const date = new Date(value);
                    const hours = date.getHours().toString().padStart(2, '0');
                    const minutes = date.getMinutes().toString().padStart(2, '0');
                    return `${hours}:${minutes}`;
                },
            },
            // axisBorder: {
            //     offsetX: 13
            // }
        };
    };
    const getYAxisOptions = () => {
        return {
            tooltip: {
                enabled: true,
            },
            labels: {
                show: true,
                formatter: (value) => {
                    if(value> 1000_000) {
                        return `${value/1000000} M`;
                    }
                    if(value> 1000) {
                        return `${value/1000} K`;
                    }
                    return value;
                },
            }
        };
    };

    return {
        chart: {
            height: volumeBarPx(window.innerHeight),
            type: 'bar',
            brush: {
                // enabled: true,
                target: 'candles'
            },
            selection: {
                enabled: true,
                // xaxis: {
                //     min: new Date('20 Jan 2017').getTime(),
                //     max: new Date('10 Dec 2017').getTime()
                // },
                fill: {
                    color: '#ccc',
                    opacity: 0.4
                },
                stroke: {
                    color: '#0D47A1',
                }
            },
        },
        dataLabels: {
            enabled: false
        },
        plotOptions: {
            bar: {
                horizontal: false,
                dataLabels: {
                    position: 'top',
                },
            }
        },
        // plotOptions: {
        //     // bar: {
        //     //
        //     // }
        //     // bar: {
        //     //     horizontal: false,
        //     //     columnWidth: '80%',
        //     //     colors: {
        //     //         ranges: [{
        //     //             from: -1000,
        //     //             to: 0,
        //     //             color: '#F15B46'
        //     //         }, {
        //     //             from: 1,
        //     //             to: 10000,
        //     //             color: '#FEB019'
        //     //         }],
        //     //     },
        //     // }
        // },
        stroke: {
            width: 1
        },
        xaxis: getXAxisOptions(),
        yaxis: getYAxisOptions(),
    };
}