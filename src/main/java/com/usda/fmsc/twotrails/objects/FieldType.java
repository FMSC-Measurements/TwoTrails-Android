package com.usda.fmsc.twotrails.objects;

public enum FieldType
{
    ComboBox(1),
    TextBox(2),
    CheckBox(3);

    private final int value;

    FieldType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FieldType parse(int id) {
        FieldType[] types = values();
        if(types.length > id && id > -1)
            return types[id];
        throw new IllegalArgumentException("Invalid value: " + id);
    }

    public static FieldType parse(String value) {
        if (value == null) {
            throw new NullPointerException();
        }

        switch(value.toUpperCase()) {
            case "COMBOBOX": return ComboBox;
            case "TEXTBOX": return TextBox;
            case "CHECKBOX": return CheckBox;
            default: throw new RuntimeException("Unknown Value Type");
        }
    }
}