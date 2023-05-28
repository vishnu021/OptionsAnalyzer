import React, {useEffect, useState} from 'react';
import {getCandleStickOptions} from "../services/candleStickOptions";
import {getApexSeries} from "../services/chartSeries";
import {toast, ToastContainer} from "react-toastify";
import NavbarComponent from "./navbarComponent";
import {getBarOptions} from "../services/barOptions";
import ChartComponent from "./chartComponent";

function ContainerComponent(props) {

    const [exchange, setExchange] = useState("NSE");
    const [expiry, setExpiry] = useState("");
    const [symbol, setSymbol] = useState("tatamotors");
    const [date, setDate] = useState("2023-05-26");

    const [barOptions, setBarOptions] = useState({});
    const [chartOptions, setChartOptions] = useState({});
    const [chartSeries, setChartSeries] = useState([]);
    const [barSeries, setBarSeries] = useState([]);


    const onFormSubmit = async (event) => {
        event.preventDefault();
        await updateGraph();
    }

    const updateGraph = async () => {
        try {
            const {series, seriesBar} = await getApexSeries(date, symbol);
            setChartSeries(series);
            setChartOptions(getCandleStickOptions(series));
            setBarOptions(getBarOptions());
            setBarSeries(seriesBar);
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
                onFormSubmit={onFormSubmit}
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