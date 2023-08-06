const navbarPercentage = 6;
const statusBarPercentage = 4;
const volumeBarPercentage = 12;
const candleStickBarPercentage = 96 - navbarPercentage
    - statusBarPercentage
    - volumeBarPercentage;

export const navbarPx = (screenHeight) => {
    return (screenHeight * navbarPercentage) / 100;
}
export const statusPx = (screenHeight) => {
    return (screenHeight * statusBarPercentage) / 100;
}

export const candleStickPx = (screenHeight) => {
    return (screenHeight * candleStickBarPercentage) / 100;
}

export const volumeBarPx = (screenHeight) => {
    return (screenHeight * volumeBarPercentage) / 100;
}