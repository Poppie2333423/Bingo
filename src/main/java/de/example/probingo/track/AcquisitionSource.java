package de.example.probingo.track;

public enum AcquisitionSource {
    PICKUP("pickup"),
    CRAFT("craft"),
    SMELT("smelt"),
    FISH("fish"),
    TRADE("trade"),
    LOOT("loot"),
    DROP("pickup");

    private final String displayKey;

    AcquisitionSource(String displayKey) {
        this.displayKey = displayKey;
    }

    public String displayKey() {
        return displayKey;
    }
}
