package ru.kofa.demo.enums;

public enum UnfriendlyCountry {
    UNITED_STATES("United States of America"),
    UNITED_KINGDOM("United Kingdom"),
    CANADA("Canada"),
    JAPAN("Japan"),
    AUSTRALIA("Australia"),
    UKRAINE("Ukraine"),
    NORWAY("Norway"),
    SWITZERLAND("Switzerland"),
    SOUTH_KOREA("Korea, Republic of");

    private final String name;

    UnfriendlyCountry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
