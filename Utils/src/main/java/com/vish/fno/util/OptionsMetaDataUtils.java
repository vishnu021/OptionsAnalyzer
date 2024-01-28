package com.vish.fno.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.vish.fno.util.Constants.NIFTY_50;
import static com.vish.fno.util.Constants.NIFTY_BANK;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OptionsMetaDataUtils {

    public static int getQuantity(String symbol) {
        return switch (symbol) {
            case NIFTY_BANK -> 15;
            case NIFTY_50 -> 50;
            case "BAJFINANCE" -> 125;
            case "HDFCBANK" -> 550;
            case "HINDUNILVR" -> 300;
            case "RELIANCE" -> 250;
            default -> 0;
        };
    }
}
