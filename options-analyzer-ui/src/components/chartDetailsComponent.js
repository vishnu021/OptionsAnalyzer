import React from 'react';
import {statusPx} from "../services/windowHeightService";

function ChartDetailsComponent({chartData}) {
    return (
        <div
            className="navbar-fixed-top p-2"
            style={{ height: statusPx(window.innerHeight), backgroundColor: "lightblue"}}
        >
           <b> symbol: </b>{' '}{chartData.symbol}{' '}
            <b>date: </b>{' '}{chartData.date}{' '}{' '}
            <b>instrument:</b>{' '}{chartData.instrument}({chartData.dataPoints})
        </div>
    );
}

export default ChartDetailsComponent;