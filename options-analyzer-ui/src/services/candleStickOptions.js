export const getCandleStickOptions = (series) => {

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
        };
    };

    const getYAxisOptions = () => {
        let min = 0;
        let max = 0;

        if(series && series.length > 0) {
            min = Math.min(...series[0].data.map((d) => Math.min(...d.y))) - 5;
            max = Math.max(...series[0].data.map((d) => Math.max(...d.y))) + 5;
        }

        return {
            tooltip: {
                enabled: true,
            },
            labels: {
                formatter: (value) => `${value.toFixed(2)}`,
            },
            min,
            max,
        };
    };

    return  {
        chart: {
            type: 'candlestick',
            height: 350,
            id: 'candles',
            toolbar: {
                autoSelected: 'pan',
                show: false
            },
            zoom: {
                enabled: false
            },
        },
        // title: {
        //     text: 'CandleStick Chart',
        //     align: 'left',
        // },
        plotOptions: {
            candlestick: {
                colors: {
                    upward: '#21cc5f',
                    downward: '#f84406'
                }
            }
        },
        xaxis: getXAxisOptions(),
        yaxis : getYAxisOptions(),
    };

}