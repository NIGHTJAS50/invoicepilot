package com.nightjas50.invoicepilot.domain;

import java.util.Objects;
import java.util.UUID;

public final class Client {
    private final String id;
    private final String name;
    private final String email;
    private final String company;
    private final String address;

    public Client(String id, String name, String email, String company, String address) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.email = requireText(email, "email");
        this.company = company == null ? "" : company;
        this.address = address == null ? "" : address;
    }

    public static Client create(String name, String email, String company, String address) {
        return new Client("client_" + UUID.randomUUID().toString().substring(0, 8), name, email, company, address);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String company() {
        return company;
    }

    public String address() {
        return address;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof Client client)) {
            return false;
        }
        return id.equals(client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
