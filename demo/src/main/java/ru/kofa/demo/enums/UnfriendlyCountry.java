package ru.kofa.demo.enums;

import java.util.Arrays;
import java.util.Optional;

public enum UnfriendlyCountry {
    BELARUS("Belarus"),
    CHINA("China"),
    GERMANY("Germany"),
    TURKIYE("Türkiye"),
    FINLAND("Finland"),
    GREECE("Greece"),
    SWITZERLAND("Switzerland"),
    SPAIN("Spain"),
    SLOVAKIA("Slovakia"),
    SWEDEN("Sweden"),
    ESTONIA("Estonia"),
    KOREA_REPUBLIC("Korea, Republic of"),
    ITALY("Italy"),
    UKRAINE("Ukraine"),
    POLAND("Poland"),
    SERBIA("Serbia"),
    LITHUANIA("Lithuania"),
    DENMARK("Denmark"),
    BULGARIA("Bulgaria"),
    FRANCE("France"),
    BELGIUM("Belgium"),
    CZECH_REPUBLIC("Czech Republic"),
    CANADA("Canada"),
    SOUTH_AFRICA("South Africa"),
    THAILAND("Thailand"),
    NETHERLANDS("Netherlands"),
    UNITED_STATES("United States of America"),
    NORWAY("Norway"),
    ARMENIA("Armenia"),
    KAZAKHSTAN("Kazakhstan"),
    AUSTRIA("Austria"),
    BOSNIA_HERZEGOVINA("Bosnia and Herzegovina"),
    HUNGARY("Hungary"),
    JAPAN("Japan"),
    LATVIA("Latvia"),
    LUXEMBOURG("Luxembourg"),
    TAIPEI_CHINESE("Taipei, Chinese"),
    INDIA("India"),
    VIET_NAM("Viet Nam"),
    SLOVENIA("Slovenia"),
    UNITED_KINGDOM("United Kingdom");

    private final String name;

    UnfriendlyCountry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static String[] getAllNames() {
        return Arrays.stream(values())
                .map(UnfriendlyCountry::getName)
                .toArray(String[]::new);
    }


    public static Optional<UnfriendlyCountry> findByName(String countryName) {
        return Arrays.stream(values())
                .filter(country -> country.getName().equalsIgnoreCase(countryName))
                .findFirst();
    }

    @Override
    public String toString() {
        return name;
    }
}
