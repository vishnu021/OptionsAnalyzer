import React from 'react';

function NavbarComponent({handleExchangeChange, onSymbolUpdate, symbol, handleSymbolChange, filteredOptions, handleOptionClick, date, handleDateChange}) {

    const intervals = ["3minute", "5minute", "10minute", "15minute", "30minute", "60minute", "day"];
    const getIntervalOptions = () => {
        return intervals.map(i => <option className="form-control" key={i} value={i}>{i}</option>)
    }
    return (
        <nav className="navbar navbar-expand-lg navbar-fixed-top" style={{ zIndex: 1 }}>
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
                    <option value="BSE">BSE</option>
                    <option value="NFO">NFO</option>
                </select>
                <div className="dropdown form-control">
                    <input
                        type="text"
                        value={symbol}
                        onChange={handleSymbolChange}
                        className="form-control"
                        data-bs-toggle="dropdown"
                        style={{backgroundColor: "#cfe2ef", width: "200px"}}
                    />
                    {filteredOptions.length > 0 && (
                        <div className="dropdown-menu show">
                            {filteredOptions.map((option) => (
                                <button
                                    key={option}
                                    className="dropdown-item"
                                    type="button"
                                    onClick={() => handleOptionClick(option)}
                                >
                                    {option}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
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
                    placeholder="Interval">
                    <option className="form-control" value="minute" defaultValue="minute">minute</option>
                    {getIntervalOptions()}
                </select>

                <button
                    className="form-control btn"
                    type="submit"
                    onClick={onSymbolUpdate}
                    style={{backgroundColor: "blue", color: "white"}} >
                    Update
                </button>
            </form>
        </nav>
    );
}

export default NavbarComponent;