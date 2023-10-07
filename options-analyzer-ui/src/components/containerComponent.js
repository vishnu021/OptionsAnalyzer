import React, {useEffect, useState} from 'react';
import {getCandleStickOptions} from "../services/candleStickOptions";
import {getApexSeries} from "../services/chartSeries";
import {toast, ToastContainer} from "react-toastify";
import NavbarComponent from "./navbarComponent";
import {getBarOptions} from "../services/barOptions";
import ChartComponent from "./chartComponent";
import ChartDetailsComponent from "./chartDetailsComponent";

function ContainerComponent(props) {

    const [exchange, setExchange] = useState("NSE");
    const [expiry, setExpiry] = useState("");
    const [symbol, setSymbol] = useState("tatamotors");
    const [date, setDate] = useState("2023-05-26");
    const [interval, setInterval] = useState("1");

    const [barOptions, setBarOptions] = useState({});
    const [chartOptions, setChartOptions] = useState({});
    const [chartSeries, setChartSeries] = useState([]);
    const [barSeries, setBarSeries] = useState([]);
    const [chartData, setChartData] = useState({});


    const onFormSubmit = async (event) => {
        event.preventDefault();
        await updateGraph();
    }

    const updateGraph = async () => {
        try {
            const chartData = await getApexSeries(date, symbol, interval);
            setChartData(chartData);
            setChartSeries(chartData.series);
            setChartOptions(getCandleStickOptions(chartData.series));
            setBarOptions(getBarOptions());
            setBarSeries(chartData.seriesBar);
        } catch (e) {
            console.error("Failed with error : ", e.message);
            toast.error(`Failed : ${e.message}`);
        }
    };

    useEffect(() => {
        return async () => {
            await updateGraph();
        };
    }, []);


    return (
        <div style={{width: "100%", backgroundColor: "#cfe2ef"}}>
            <ToastContainer />
            <NavbarComponent
                exchange={exchange}
                setExchange={setExchange}
                expiry={expiry}
                setExpiry={setExpiry}
                symbol={symbol}
                setSymbol={setSymbol}
                date={date}
                setDate={setDate}
                interval={interval}
                setInterval={setInterval}
                onFormSubmit={onFormSubmit}
            />
            <ChartDetailsComponent
                chartData = {chartData}
            />
            <ChartComponent
                series={chartSeries}
                options={chartOptions}
                seriesBar={barSeries}
                optionsBar={barOptions}
            />
        </div>
    );
}

export default ContainerComponent;