package com.wiredid.skytree.banking.util;

import com.wiredid.skytree.SkytreePlugin;

import java.text.DecimalFormat;

public class BankUtil {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static SkytreePlugin plugin;

    public static void setPlugin(SkytreePlugin p) {
        plugin = p;
    }

    private static long getCentsPerUnit() {
        return plugin != null ? plugin.getConfig().getLong("bank.cents_per_unit", 100) : 100;
    }

    private static double getFeePercent() {
        return plugin != null ? plugin.getConfig().getDouble("bank.transaction_fee_percent", 0.01) : 0.01;
    }

    private static long getMinFeeCents() {
        return plugin != null ? plugin.getConfig().getLong("bank.min_fee_cents", 1) : 1;
    }

    public static long toCents(double USDT) {
        return (long) (USDT * getCentsPerUnit());
    }

    public static double toUSDT(long cents) {
        return cents / (double) getCentsPerUnit();
    }

    public static String formatCurrency(long cents) {
        return "\u20AE " + MONEY_FORMAT.format(toUSDT(cents));
    }

    @Deprecated
    public static String formatBTC(long cents) {
        return "\u20AE " + MONEY_FORMAT.format(toUSDT(cents));
    }

    public static long calculateFee(long amount) {
        long fee = (long) (amount * (getFeePercent() / 100.0));
        return Math.max(fee, getMinFeeCents());
    }

    public static long calculateInterest(long balance, double ratePerMinute, long minutes) {
        return (long) (balance * ratePerMinute * minutes);
    }
}
