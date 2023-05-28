import React, {createContext, useEffect, useState} from 'react';
import http from "../services/httpService";
import {toast, ToastContainer} from "react-toastify";
import ContainerComponent from "./containerComponent";

import 'bootstrap/dist/css/bootstrap.css'
import 'react-toastify/dist/ReactToastify.css';
import '../styles/App.css';

const allInstrumentUrl = "https://127.0.0.1:8080/api/v1/allInstruments/";

export const InstrumentsContext = createContext(null);

function App() {
  const [instruments, setInstruments] = useState([]);

  useEffect(() => {
    const getAllInstruments = async () => {
      const symbolData = await http.get(`${allInstrumentUrl}`);
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
