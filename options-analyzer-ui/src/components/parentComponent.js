import React, {useEffect, useState} from 'react';
import {getCandleStickOptions} from "../services/candleStickOptions";
import {getApexSeries} from "../services/chartSeries";
import ReactApexChart from 'react-apexcharts';
import {toast, ToastContainer} from "react-toastify";
import NavbarComponent from "./navbarComponent";
import 'react-toastify/dist/ReactToastify.css';
import {getBarOptions} from "../services/barOptions";
import http from "../services/httpService";

const symbolsUrl = "https://127.0.0.1:8080/api/v1/exchangeSymbols/";

function ParentComponent(props) {

    const [exchange, setExchange] = useState("NSE");
    const [symbol, setSymbol] = useState("tatamotors");
    const [date, setDate] = useState("2023-05-12");
    const [allSymbols, setAllSymbols] = useState({});
    const [filteredSymbols, setFilteredSymbols] = useState({});

    const [options, setOptions] = useState({});
    const [optionsBar, setOptionsBar] = useState({});
    const [series, setSeries] = useState([]);
    const [seriesBar, setSeriesBar] = useState([]);


    const onSymbolUpdate = async (event) => {
        event.preventDefault();
        await updateGraph();
    }

    const updateGraph = async () => {
        try {
            const {series, seriesBar} = await getApexSeries(date, symbol);
            const options = getCandleStickOptions(series);
            const optionsBar = getBarOptions();
            setOptions(options);
            setOptionsBar(optionsBar);
            setSeries(series);
            setSeriesBar(seriesBar);
        } catch (e) {
            console.error("Failed with error : ", e.message);
            toast.error(e.message);
        }
    };

    useEffect(() => {
        const fetchData = async () => {
            const {data} = await http.get(`${symbolsUrl}/${exchange}`);
            setAllSymbols(Object.keys(data));
        };
        fetchData();
    }, [exchange]);

    useEffect(() => {
        return async () => {
            await updateGraph();
        };
    }, []);


    const handleSymbolChange = (event) => {
        const newSearchTerm = event.target.value;
        setSymbol(newSearchTerm);
        const filtered = allSymbols.filter((option) =>
            option.toLowerCase().includes(newSearchTerm.toLowerCase())
        );
        setFilteredSymbols(filtered);
    };

    const handleDateChange = ({currentTarget:input}) => {
        setDate(input.value);
    };

    const handleExchangeChange = ({currentTarget:input}) => {
        setExchange(input.value);
    };

    const handleOptionClick = (option) => {
        setSymbol(option);
        setFilteredSymbols([]);
    };

    return (
        <div style={{width: "100%", backgroundColor: "#cfe2ef"}}>
            <ToastContainer />
            <NavbarComponent
                exchange={exchange}
                handleExchangeChange={handleExchangeChange}
                onSymbolUpdate={onSymbolUpdate}
                symbol={symbol}
                handleSymbolChange={handleSymbolChange}
                handleOptionClick={handleOptionClick}
                filteredOptions={filteredSymbols}
                date={date}
                handleDateChange={handleDateChange}
            />
            <div className="chart-box" style={{ zIndex: 0 }}>
                <div id="chart-candlestick">
                    <ReactApexChart options={options} series={series} type="candlestick" height={600} width="100%" />
                </div>
                <div id="chart-bar">
                    <ReactApexChart options={optionsBar} series={seriesBar} type="bar" height={150} width="100%" />
                </div>
            </div>
        </div>
    );
}

export default ParentComponent;