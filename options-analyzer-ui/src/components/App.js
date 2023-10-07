import React, {createContext, useEffect, useState} from 'react';
import http from "../services/httpService";
import {toast, ToastContainer} from "react-toastify";
import ContainerComponent from "./containerComponent";

import 'bootstrap/dist/css/bootstrap.css'
import 'react-toastify/dist/ReactToastify.css';
import '../styles/App.css';

export const InstrumentsContext = createContext(null);

function App() {
  const [instruments, setInstruments] = useState([]);

  useEffect(() => {
    const getAllInstruments = async () => {
        const chartEndpoint = `${process.env.REACT_APP_ALL_INSTRUMENT_ENDPOINT}`
        const symbolData = await http.get(`${chartEndpoint}`);

      if(symbolData) {
        const {data} = symbolData;
        setInstruments(data);
      } else {
        toast.error(`no data available for request`);
      }
    };

    getAllInstruments();
  }, []);


  return (
      <React.Fragment>
        <ToastContainer />
        <InstrumentsContext.Provider value={{ instruments }} >
          <ContainerComponent />
        </InstrumentsContext.Provider>
      </React.Fragment>
  );
}

export default App;
