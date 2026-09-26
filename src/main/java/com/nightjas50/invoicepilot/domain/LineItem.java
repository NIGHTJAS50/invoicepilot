package com.nightjas50.invoicepilot.domain;

import java.math.BigDecimal;

public final class LineItem {
    private final String description;
    private final BigDecimal quantity;
    private final Money unitPrice;

    public LineItem(String description, BigDecimal quantity, Money unitPrice) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice == null || unitPrice.isNegative()) {
            throw new IllegalArgumentException("unit price cannot be negative");
        }
        this.description = description.trim();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String description() {
        return description;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public Money unitPrice() {
        return unitPrice;
    }

    public Money total() {
        return unitPrice.multiply(quantity);
    }
}
