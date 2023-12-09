import http from "./httpService";

const datePattern = /^\d{4}-\d{2}-\d{2}$/;

export const getApexSeries = async (date, symbol, interval) => {
    if (!symbol) throw new Error("Invalid Symbol");
    if (!datePattern.test(date)) throw new Error("Please use yyyy-mm-dd format");

    const chartEndpoint = `${process.env.REACT_APP_CHART_ENDPOINT}/${date}/${symbol}/${interval}`
    const {data} = await http.get(`${chartEndpoint}`);
    return data;
}
export const getApexOptionSeries = async (date, symbol, interval) => {
    if (!symbol) throw new Error("Invalid Symbol");
    if (!datePattern.test(date)) throw new Error("Please use yyyy-mm-dd format");

    const chartEndpoint = `${process.env.REACT_APP_OPTION_CHART_ENDPOINT}/${date}/${symbol}/${interval}`
    const {data} = await http.get(`${chartEndpoint}`);
    return data;
}