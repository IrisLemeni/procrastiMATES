package com.example.procrastimates;

public enum Priority {
    LOW(0), MEDIUM(1), HIGH(2);

    private final int value;

    Priority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
