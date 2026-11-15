package model;

public enum TaskSize {
    SMALL(5, "Small"),
    MEDIUM(15, "Medium"),
    LARGE(30, "Large"),
    VERY_LARGE(60, "Very large");

    private final int units;
    private final String label;

        TaskSize(int units, String label) {
        this.units = units;
        this.label = label;
    }

    public int getUnits() {
        return units;
    }

    public String getLabel() {
        return label;
    }
}

