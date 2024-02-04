package com.vish.fno.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    public static String getStringRoundedPrice(final double price) {
        boolean signed = price < 0;
        double absolutePrice = Math.abs(price);
        String simplifiedPrice;

        if (absolutePrice > (1000000)) {
            simplifiedPrice = getFormattedRoundPrice(absolutePrice / (1000 * 1000)) + "M";
        }
        else if (absolutePrice > 1000) {
            simplifiedPrice = getFormattedRoundPrice(absolutePrice / 1000) + "K";
        }
        else {
            simplifiedPrice = getFormattedRoundPrice(absolutePrice);
        }

        return signed ? "-" + simplifiedPrice : simplifiedPrice;
    }

    public static String format(double price) {
        return String.format("%.2f", price);
    }

    private static String getFormattedRoundPrice(double price) {
        return format(round(price));
    }

    public static double round(double price) {
        return Math.round(price / 0.05) * 0.05;
    }

}
