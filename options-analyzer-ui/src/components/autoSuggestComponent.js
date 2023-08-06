import React, {useState} from 'react';

function AutoSuggestComponent({value, setValue, allValues, width}) {

    const [filteredValues, setFilteredValues] = useState([]);
    const [showAutoSuggest, setShowAutoSuggest] = useState(false);

    const onChangeHandler = (event) => {
        const searchTerm = event.target.value;
        let filtered = allValues
                .filter((obj) => obj.toLowerCase().includes(searchTerm.toLowerCase()))
                .map(obj => obj);
        setFilteredValues(filtered);
        setValue(searchTerm);
    };

    const onOptionClickHandler = (optionValue) => {
        setValue(optionValue);
        setShowAutoSuggest(false);
    };

    const onClickHandler = () => {
        if(!showAutoSuggest) {
            setShowAutoSuggest(true);
            setFilteredValues(allValues);
        }
    }

    return (
       <React.Fragment>
           <div className="dropdown form-control">
               <input
                   type="text"
                   value={value}
                   onChange={onChangeHandler}
                   onClick={onClickHandler}
                   className="form-control"
                   data-bs-toggle="dropdown"
                   style={{backgroundColor: "#cfe2ef", width: width}}
               />
               {filteredValues.length > 0 && showAutoSuggest && (
                   <div
                       className="dropdown-menu show"
                       style={{maxHeight: "500px", overflowY: "auto"}}
                   >
                       {filteredValues.map((option) => (
                           <button
                               key={option}
                               className="dropdown-item"
                               type="button"
                               onClick={() => onOptionClickHandler(option)}
                           >
                               {option}
                           </button>
                       ))}
                   </div>
               )}
           </div>
       </React.Fragment>
    );
}

export default AutoSuggestComponent;