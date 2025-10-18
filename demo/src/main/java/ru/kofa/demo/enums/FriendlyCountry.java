package ru.kofa.demo.enums;

import java.util.Arrays;
import java.util.Optional;

public enum FriendlyCountry {
    BELARUS("Беларусь"),
    CHINA("Китай"),
    INDIA("Индия"),
    KAZAKHSTAN("Казахстан"),
    UZBEKISTAN("Узбекистан"),
    BRAZIL("Бразилия"),
    SOUTH_AFRICA("Южная Африка"),
    IRAN("Иран"),
    SYRIA("Сирия"),
    VENEZUELA("Венесуэла"),
    CUBA("Куба"),
    NORTH_KOREA("КНДР");

    private final String name;

    FriendlyCountry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static String[] getAllNames() {
        return Arrays.stream(values())
                .map(FriendlyCountry::getName)
                .toArray(String[]::new);
    }

    public static Optional<FriendlyCountry> findByName(String countryName) {
        return Arrays.stream(values())
                .filter(country -> country.getName().equalsIgnoreCase(countryName))
                .findFirst();
    }

    @Override
    public String toString() {
        return name;
    }
}
