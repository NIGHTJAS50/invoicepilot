package com.nightjas50.invoicepilot.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public final class Money implements Comparable<Money> {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "amount");
        this.currency = Currency.getInstance(currencyCode.toUpperCase());
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), currencyCode);
    }

    public static Money zero(String currencyCode) {
        return of("0", currencyCode);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency.getCurrencyCode());
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency.getCurrencyCode());
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), currency.getCurrencyCode());
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currencyCode() {
        return currency.getCurrencyCode();
    }

    public String format() {
        return currencyCode() + " " + amount.toPlainString();
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Money values must use the same currency");
        }
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof Money money)) {
            return false;
        }
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
}
