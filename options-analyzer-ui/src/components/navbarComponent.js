import React, {useContext} from 'react';
import AutoSuggestComponent from "./autoSuggestComponent";
import {InstrumentsContext} from "./App";
import {navbarPx} from "../services/windowHeightService";

function NavbarComponent({exchange, setExchange,
                             expiry, setExpiry,
                             symbol, setSymbol,
                             date, setDate,
                             interval, setInterval,
                             onFormSubmit}) {

    const {instruments} = useContext(InstrumentsContext);

    const getIntervalOptions = () => {
        // Create a mapping from the original value to the new value
        const intervalValueMap = {
            "minute": "1",
            "3minute": "3",
            "5minute": "5",
            "10minute": "10",
            "15minute": "15",
            "30minute": "30",
            "60minute": "60",
            "day": "375"
        };

        // Map through the keys in the intervalValueMap and create the option elements
        return Object.keys(intervalValueMap).map(i => (
            <option
                className="form-control"
                key={i}
                value={intervalValueMap[i]} >
                {i}
            </option>
        ));
    };

    const handleDateChange = ({currentTarget:input}) => {
        setDate(input.value);
    };

    const handleIntervalChange = ({currentTarget:input}) => {
        setInterval(input.value);
    };

    const handleExchangeChange = ({currentTarget:input}) => {
        setExchange(input.value);
    };

    const getExpiryDates = () => {
        const expiryDates = instruments
            .filter(obj => obj.exchange === 'NFO')
            .map(obj => obj.expiry);

        return sortAndCreateArray(expiryDates);
    }

    const getSymbols = () => {
        let symbols = instruments.filter(obj => obj.exchange === exchange)

        if(exchange==='NFO') {
            symbols = symbols.filter(obj => obj.expiry === expiry)
        }

        symbols = symbols.map(obj => obj.symbol);
        return sortAndCreateArray(symbols);
    }

    const sortAndCreateArray = (elements) => {
        elements.sort((a, b) => a.localeCompare(b));
        return Array.from(new Set(elements));
    }

    return (
        <nav className="navbar navbar-expand-lg navbar-fixed-top" style={{/* zIndex: 1,*/ height: navbarPx(window.innerHeight) }}>
            <h4 className="m-1 p-1">Options Analyzer</h4>
            <form className="d-flex" >
                <select
                    className="form-select form-control custom-select custom-select-sm"
                    placeholder="Interval"
                    aria-label="Default select example"
                    onChange={handleExchangeChange}
                    style={{backgroundColor: "#cfe2ef"}}
                >
                    <option value="NSE" defaultValue="NSE">NSE</option>
                    <option value="NFO">NFO</option>
                </select>
                <AutoSuggestComponent
                    value={expiry}
                    setValue={setExpiry}
                    allValues={getExpiryDates()}
                    width={"120px"}
                />
                <AutoSuggestComponent
                    value={symbol}
                    setValue={setSymbol}
                    allValues={getSymbols()}
                    width={"220px"}
                />
                <input
                    className="form-control"
                    type="search"
                    placeholder="Date"
                    aria-label="Symbol"
                    onChange={handleDateChange}
                    value={date}
                    style={{backgroundColor: "#cfe2ef", width: "120px"}}
                    />

                <select
                    className="form-select form-control"
                    placeholder="Interval"
                    onChange={handleIntervalChange}
                    value={interval}
                    // defaultValue={interval}
                >
                    {getIntervalOptions()}
                </select>

                <button
                    className="form-control btn"
                    type="submit"
                    onClick={onFormSubmit}
                    style={{backgroundColor: "blue", color: "white"}} >
                    Update
                </button>
            </form>
        </nav>
    );
}

export default NavbarComponent;