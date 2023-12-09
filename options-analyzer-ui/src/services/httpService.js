import axios from "axios";
import {toast} from "react-toastify";

axios.interceptors.response.use(null, error => {
    const expectedError = error.response && error.response.status >=400 && error.response.status<500
    if(!expectedError) {
        toast.error('An unexpected error occurred')
    }
    return Promise.reject(error);
});

function getBaseURL() {
    if (process.env.NODE_ENV === "development") {
        return "https://127.0.0.1:8080";
    } else {
        return `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}`;
    }
}

axios.defaults.baseURL = getBaseURL();

function getServiceEndpoint(endpoint) {
    return `${axios.defaults.baseURL}${endpoint}`;
}

export default {
    get: (url, config) => axios.get(getServiceEndpoint(url), config),
    post: (url, data, config) => axios.post(getServiceEndpoint(url), data, config),
    put: (url, data, config) => axios.put(getServiceEndpoint(url), data, config),
    delete: (url, config) => axios.delete(getServiceEndpoint(url), config)
};