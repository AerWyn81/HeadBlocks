package fr.aerwyn81.headblocks.databases;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum EnumTypeDatabase {
    SQLite("SQLite"),
    MySQL("MySQL");

    private final String value;

    EnumTypeDatabase(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static List<String> toStringList() {
        return Arrays.stream(EnumTypeDatabase.values()).map(EnumTypeDatabase::getValue).collect(Collectors.toList());
    }
}
