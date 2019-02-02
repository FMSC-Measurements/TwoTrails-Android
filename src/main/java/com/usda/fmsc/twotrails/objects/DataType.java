package com.usda.fmsc.twotrails.objects;

public enum DataType
{
    INTEGER(0),
    DECIMAL(1),
    FLOAT(2),
    TEXT(3),
    BYTE_ARRAY(4),
    BOOLEAN(5);

    private final int value;

    DataType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DataType parse(int id) {
        DataType[] types = values();
        if(types.length > id && id > -1)
            return types[id];
        throw new IllegalArgumentException("Invalid value: " + id);
    }

    public static DataType parse(String value) {
        if (value == null) {
            throw new NullPointerException();
        }

        switch(value.toUpperCase()) {
            case "INTEGER": return INTEGER;
            case "DECIMAL": return DECIMAL;
            case "FLOAT": return FLOAT;
            case "TEXT": return TEXT;
            case "BYTE_ARRAY": return BYTE_ARRAY;
            case "BOOLEAN": return BOOLEAN;
            default: throw new RuntimeException("Unknown Data Type");
        }
    }
}
