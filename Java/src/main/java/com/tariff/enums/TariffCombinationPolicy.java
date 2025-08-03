package com.tariff.enums;

public enum TariffCombinationPolicy {
    ADDITIVE("ADDITIVE"),
    MAXIMUM("MAXIMUM"),
    MINIMUM("MINIMUM"),
    ITEM("ITEM"),
    COMPONENT("COMPONENT");

    private final String value;

    TariffCombinationPolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
