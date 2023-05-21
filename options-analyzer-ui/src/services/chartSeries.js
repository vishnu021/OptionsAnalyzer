import http from "./httpService";

const datePattern = /^\d{4}-\d{2}-\d{2}$/;
const chartUrl = "https://127.0.0.1:8080/api/v1/apexChart";

export const getApexSeries = async (date, symbol) => {
    if (!symbol) throw new Error("Invalid Symbol");
    if (!datePattern.test(date)) throw new Error("Please use yyyy-mm-dd format");

    const {data} = await http.get(`${chartUrl}/${date}/${symbol}`);
    return data;
}